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
 * <p>
 * Con la tabla congelada se sirve la del momento del congelamiento: solo cambia por envíos hechos antes
 * de congelar que se califican después, hasta que el organizador la libere.
 * </p>
 */
@Component
public class ScoreboardCache {

    /**
     * Tabla calculada desde la base de datos.
     *
     * @param rows   Filas ordenadas.
     * @param frozen {@code true} si es la tabla congelada.
     */
    public record Board(List<ScoreboardRow> rows, boolean frozen) {
    }

    private final Map<UUID, List<ScoreboardRow>> boards = new HashMap<>();
    private final Map<UUID, List<ScoreboardRow>> frozenBoards = new HashMap<>();
    private final Map<UUID, List<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    /**
     * Tabla de posiciones del concurso (la congelada si lo está); la construye con {@code builder} si no está en caché.
     *
     * @param contestId Identificador del concurso.
     * @param builder   Cálculo completo desde la base de datos.
     * @return Copia de la tabla ordenada.
     */
    public synchronized List<ScoreboardRow> getOrBuild(UUID contestId, Function<UUID, Board> builder) {
        List<ScoreboardRow> rows = frozenBoards.containsKey(contestId) ? frozenBoards.get(contestId) : boards.get(contestId);
        if (rows == null) {
            Board board = builder.apply(contestId);
            rows = new ArrayList<>(board.rows());
            (board.frozen() ? frozenBoards : boards).put(contestId, rows);
        }
        return List.copyOf(rows);
    }

    /**
     * Congela la tabla del concurso y la envía completa a los suscriptores.
     *
     * @param contestId Identificador del concurso.
     * @param rows      Tabla al momento del congelamiento.
     */
    public void freeze(UUID contestId, List<ScoreboardRow> rows) {
        List<ScoreboardRow> frozen = new ArrayList<>(rows);
        synchronized (this) {
            frozenBoards.put(contestId, frozen);
            boards.remove(contestId);
        }
        broadcastBoard(contestId, true, List.copyOf(frozen));
    }

    /**
     * Libera la tabla del concurso y envía la real completa a los suscriptores.
     *
     * @param contestId Identificador del concurso.
     * @param rows      Tabla real, ya calculada con todos los envíos.
     */
    public void unfreeze(UUID contestId, List<ScoreboardRow> rows) {
        List<ScoreboardRow> live = new ArrayList<>(rows);
        synchronized (this) {
            frozenBoards.remove(contestId);
            boards.put(contestId, live);
        }
        broadcastBoard(contestId, false, List.copyOf(live));
    }

    private void broadcastBoard(UUID contestId, boolean frozen, List<ScoreboardRow> rows) {
        List<Object> table = rows.stream().map(r -> (Object) TeamWebMapper.toResponse(r.team())).toList();
        subscribers.getOrDefault(contestId, List.of()).forEach(emitter -> {
            send(contestId, emitter, "frozen", frozen);
            send(contestId, emitter, "scoreboard", table);
        });
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
            // Congelada, solo llegan cambios de envíos anteriores al congelamiento (ver UpdateTeamScoreUseCaseImpl).
            List<ScoreboardRow> board = frozenBoards.containsKey(contestId) ? frozenBoards.get(contestId) : boards.get(contestId);
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
     * Suscribe un cliente al concurso y le envía si está congelada y la tabla completa; después solo recibe cambios.
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
            List<Team> table = current.get();
            boolean frozen;
            synchronized (this) {
                frozen = frozenBoards.containsKey(contestId);
            }
            send(contestId, emitter, "frozen", frozen);
            send(contestId, emitter, "scoreboard", table.stream().map(TeamWebMapper::toResponse).toList());
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
