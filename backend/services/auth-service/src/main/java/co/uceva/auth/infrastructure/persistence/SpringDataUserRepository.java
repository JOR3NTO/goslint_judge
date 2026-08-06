package co.uceva.auth.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Interfaz de Spring Data JPA.
 * Permite realizar operaciones CRUD sobre la tabla 'users' y provee
 * la implementación automática de consultas comunes sin necesidad de escribir SQL.
 */
@Repository
public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {
    
    /** 
     * Busca en base de datos la entidad cuyo email coincida.
     * Es resuelto internamente por Spring Data mediante el nombre del método. 
     */
    Optional<UserJpaEntity> findByEmail(String email);

    /** Busca en base de datos la entidad por su username. */
    Optional<UserJpaEntity> findByUsername(String username);

    /** 
     * Ejecuta una consulta optimizada para saber si existe el email. 
     * Retorna un booleano, mucho más rápido que traer toda la entidad.
     */
    boolean existsByEmail(String email);

    /** Verifica si existe un username. */
    boolean existsByUsername(String username);
}
