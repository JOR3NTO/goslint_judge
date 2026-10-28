package co.uceva.contest.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.uceva.contest.domain.model.Contest;

/**
 * Puerto de salida (contrato de repositorio) para la entidad {@link Contest}.
 * Define las operaciones de persistencia sin acoplarse a una tecnología específica.
 */
public interface ContestRepository {

    /**
     * Guarda un concurso y retorna la instancia persistida.
     *
     * @param contest Concurso a persistir.
     * @return El concurso persistido.
     */
    Contest save(Contest contest);

    /**
     * Elimina un concurso dado su identificador único.
     *
     * @param contestId Identificador del concurso a eliminar.
     */
    void deleteById(UUID contestId);

    /**
     * Busca un concurso por su identificador único.
     *
     * @param contestId Identificador del concurso.
     * @return {@link Optional} con el concurso si existe, o vacío en caso contrario.
     */
    Optional<Contest> findById(UUID contestId);

    /**
     * Busca todos los concursos creados por un usuario específico.
     *
     * @param createdBy Identificador del usuario creador.
     * @return Lista de concursos creados por dicho usuario.
     */
    List<Contest> findAllByCreatedBy(UUID createdBy);

    /**
     * Busca concursos cuyo título contenga el valor dado, sin distinguir
     * mayúsculas de minúsculas (coincidencia parcial, no exacta).
     *
     * @param title Fragmento del título a buscar.
     * @return Lista de concursos cuyo título contiene el fragmento.
     */
    List<Contest> findAllByTitle(String title);

    /**
     * Recupera todos los concursos registrados en la plataforma.
     *
     * @return Lista de todos los concursos.
     */
    List<Contest> findAll();
}

