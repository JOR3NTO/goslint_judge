package co.uceva.auth.infrastructure.web.dto;

import co.uceva.auth.domain.model.User;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de respuesta para GET /api/v1/auth/me.
 * Cumple con el contrato esperado por el frontend en ProfileService.
 */
public class UserProfileResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private String role;
    private String institution;
    private boolean isActive;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public UserProfileResponse() {}

    public static UserProfileResponse fromDomain(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.id = user.getId();
        response.firstName = user.getFirstName();
        response.lastName = user.getLastName();
        response.username = user.getUsername();
        response.email = user.getEmail();
        response.role = user.getRole().name();
        response.institution = user.getInstitution();
        response.isActive = user.isActive();
        response.createdAt = user.getCreatedAt();
        return response;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
