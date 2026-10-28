package co.uceva.auth.config;

import co.uceva.auth.model.Role;
import co.uceva.auth.model.User;
import co.uceva.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminInitializerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private SuperAdminInitializer initializer(String email, String username, String password) {
        return new SuperAdminInitializer(userRepository, passwordEncoder, email, username, password);
    }

    @Test
    void createsTheSuperAdminWhenNoneExists() {
        when(userRepository.existsByRole(Role.SUPER_ADMIN)).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("root@uceva.edu.co")).thenReturn(Optional.empty());
        when(userRepository.existsByUsernameIgnoreCase("root")).thenReturn(false);
        when(passwordEncoder.encode("Secreta1!")).thenReturn("hash");

        initializer(" Root@UCEVA.edu.co ", "root", "Secreta1!").run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals(Role.SUPER_ADMIN, saved.getValue().getRole());
        assertEquals("root@uceva.edu.co", saved.getValue().getEmail());
        assertEquals("hash", saved.getValue().getPasswordHash());
        assertTrue(saved.getValue().isActive());
    }

    @Test
    void doesNothingWhenASuperAdminAlreadyExists() {
        when(userRepository.existsByRole(Role.SUPER_ADMIN)).thenReturn(true);

        initializer("root@uceva.edu.co", "root", "Secreta1!").run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    void doesNothingWithoutConfiguration() {
        when(userRepository.existsByRole(Role.SUPER_ADMIN)).thenReturn(false);

        initializer("", "root", "").run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsAWeakPassword() {
        when(userRepository.existsByRole(Role.SUPER_ADMIN)).thenReturn(false);

        initializer("root@uceva.edu.co", "root", "debil").run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    void promotesAnExistingAccountWithThatEmail() {
        User existing = User.builder().id(UUID.randomUUID()).email("root@uceva.edu.co").role(Role.STUDENT).active(false).build();
        when(userRepository.existsByRole(Role.SUPER_ADMIN)).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("root@uceva.edu.co")).thenReturn(Optional.of(existing));

        initializer("root@uceva.edu.co", "root", "Secreta1!").run(null);

        assertEquals(Role.SUPER_ADMIN, existing.getRole());
        assertTrue(existing.isActive());
        verify(userRepository).save(existing);
    }

    @Test
    void doesNotCreateWhenTheUsernameIsTaken() {
        when(userRepository.existsByRole(Role.SUPER_ADMIN)).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("root@uceva.edu.co")).thenReturn(Optional.empty());
        when(userRepository.existsByUsernameIgnoreCase("root")).thenReturn(true);

        initializer("root@uceva.edu.co", "root", "Secreta1!").run(null);

        verify(userRepository, never()).save(any());
    }
}
