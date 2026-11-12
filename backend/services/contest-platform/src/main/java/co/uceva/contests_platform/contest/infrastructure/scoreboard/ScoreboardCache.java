package co.uceva.contests_platform.contest.infrastructure.scoreboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import co.uceva.contests_platform.contest.domain.model.ScoreboardRow;
import co.uceva.contests_platform.contest.domain.model.Team;
import co.uceva.contests_platform.contest.infrastructure.web.dto.ScoreboardChangeDTO;
import co.uceva.contests_platform.contest.infrastructure.web.mapper.TeamWebMapper;

/**
 * Tabla de posiciones en memoria por concurso y sus suscriptores SSE.
 * <p>
 * La base de datos es la única verdad: si un concurso no está en caché (arranque o reinicio)
 * se reconstruye completo desde ella. Después, cada cambio solo reposiciona al equipo afectado
 * y se envía a los suscriptores únicamente esa fila.
 * </p>
 */
@Component
public class ScoreboardCache {

    private final Map<UUID, List<ScoreboardRow>> boards = new HashMap<>();
    private final Map<UUID, List<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    /**
     * Tabla de posiciones del concurso; la construye con {@code builder} si no está en caché.
     *
     * @param contestId Identificador del concurso.
     * @param builder   Cálculo completo desde la base de datos.
     * @return Copia de la tabla ordenada.
     */
    public synchronized List<ScoreboardRow> getOrBuild(UUID contestId, Function<UUID, List<ScoreboardRow>> builder) {
        return List.copyOf(boards.computeIfAbsent(contestId, id -> new ArrayList<>(builder.apply(id))));
    }

    /**
     * Reemplaza la fila del equipo, la mueve a su nueva posición y la envía a los suscriptores.
     * Si el concurso no está en caché no hace nada: la próxima lectura lo construye ya con el cambio.
     *
     * @param row Nueva fila del equipo.
     */
    public void update(ScoreboardRow row) {
        UUID contestId = row.team().getContestId();
        int position;
        synchronized (this) {
            List<ScoreboardRow> board = boards.get(contestId);
            if (board == null) {
                return;
            }
            board.removeIf(r -> r.team().getId().equals(row.team().getId()));
            int found = Collections.binarySearch(board, row, ScoreboardRow.ORDER);
            position = found >= 0 ? found : -found - 1;
            board.add(position, row);
        }
        ScoreboardChangeDTO change = new ScoreboardChangeDTO(position + 1, TeamWebMapper.toResponse(row.team()));
        subscribers.getOrDefault(contestId, List.of()).forEach(emitter -> send(contestId, emitter, "team", change));
    }

    /**
     * Suscribe un cliente al concurso y le envía la tabla completa; después solo recibe cambios.
     * Se registra antes de leer la tabla para no perder un cambio que llegue entre ambos pasos.
     *
     * @param contestId Identificador del concurso.
     * @param current   Tabla de posiciones actual.
     * @return Emisor SSE del cliente.
     */
    public SseEmitter subscribe(UUID contestId, Supplier<List<Team>> current) {
        // Sin límite de tiempo: si un proxy corta la conexión, EventSource reconecta y recibe la tabla completa.
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        List<SseEmitter> emitters = subscribers.computeIfAbsent(contestId, id -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        try {
            send(contestId, emitter, "scoreboard", current.get().stream().map(TeamWebMapper::toResponse).toList());
        } catch (RuntimeException e) {
            emitters.remove(emitter);
            throw e;
        }
        return emitter;
    }

    private void send(UUID contestId, SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException | IllegalStateException e) {
            // El cliente se desconectó.
            subscribers.getOrDefault(contestId, List.of()).remove(emitter);
        }
    }
}
