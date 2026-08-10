package co.uceva.auth.infrastructure.persistence;

import co.uceva.auth.application.port.out.UserRepository;
import co.uceva.auth.domain.model.Role;
import co.uceva.auth.domain.model.User;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adaptador de Repositorio que implementa el Puerto de Salida de la capa de negocio.
 * Es el "puente" entre nuestro dominio (User puro) y Spring Data JPA (UserJpaEntity).
 * Al inyectarse (@Component), la capa de aplicación usa este adaptador
 * sin enterarse de que detrás existe Hibernate o Spring.
 */
@Component
public class UserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository repository;

    /** Inyección de dependencias del repositorio de Spring Data. */
    public UserRepositoryAdapter(SpringDataUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public User save(User user) {
        // Convierte el objeto de dominio a un ente que JPA entienda
        UserJpaEntity entity = toEntity(user);
        
        // Guarda en base de datos
        UserJpaEntity savedEntity = repository.save(entity);
        
        // Convierte el ente de base de datos de vuelta al dominio
        return toDomain(savedEntity);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }

    /** Mapeador manual: Transforma un modelo de Dominio a Entidad JPA */
    private UserJpaEntity toEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId());
        entity.setUsername(user.getUsername());
        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setRole(user.getRole().name()); // Se guarda como String en BD
        entity.setInstitution(user.getInstitution());
        entity.setActive(user.isActive());
        entity.setCreatedAt(user.getCreatedAt());
        return entity;
    }

    /** Mapeador manual: Transforma una Entidad JPA a un modelo de Dominio puro */
    private User toDomain(UserJpaEntity entity) {
        return User.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .passwordHash(entity.getPasswordHash())
                .role(Role.valueOf(entity.getRole())) // Convierte String a Enum
                .institution(entity.getInstitution())
                .isActive(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
