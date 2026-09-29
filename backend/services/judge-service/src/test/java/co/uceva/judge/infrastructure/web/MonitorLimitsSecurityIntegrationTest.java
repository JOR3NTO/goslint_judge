package co.uceva.judge.infrastructure.web;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.security.jwt.secret=" + MonitorLimitsSecurityIntegrationTest.SECRET,
        "app.security.jwt.issuer=goslint-judge",
        "app.security.jwt.service-id=00000000-0000-0000-0000-000000000084",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "management.health.rabbit.enabled=false"
})
@AutoConfigureMockMvc
class MonitorLimitsSecurityIntegrationTest {

    static final String SECRET = "una-clave-compartida-de-al-menos-32-bytes!!";
    private static final String URL = "/api/v1/judge/monitor-limits";
    private static final String VALID_BODY =
            "{\"outputSizeBytes\":2048,\"errorSizeBytes\":4096,\"hardTimePercent\":0.5,\"watchIntervalMs\":100,\"absoluteTimeMs\":5000}";

    @Autowired private MockMvc mockMvc;

    private static String bearer(String role) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return "Bearer " + Jwts.builder().issuer("goslint-judge").subject(UUID.randomUUID().toString())
                .claim("role", role).expiration(new Date(System.currentTimeMillis() + 60_000)).signWith(key).compact();
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void getWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void getWithInvalidTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer basura")).andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminRolesAreForbidden() throws Exception {
        for (String role : new String[] {"STUDENT", "ORGANIZER", "SERVICE"}) {
            mockMvc.perform(get(URL).header("Authorization", bearer(role))).andExpect(status().isForbidden());
            mockMvc.perform(put(URL).header("Authorization", bearer(role)).contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY)).andExpect(status().isForbidden());
        }
    }

    @Test
    void adminCanReadAndUpdate() throws Exception {
        mockMvc.perform(put(URL).header("Authorization", bearer("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY)).andExpect(status().isOk()).andExpect(jsonPath("$.outputSizeBytes").value(2048))
                .andExpect(jsonPath("$.errorSizeBytes").value(4096));

        mockMvc.perform(get(URL).header("Authorization", bearer("ADMIN"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.hardTimePercent").value(0.5))
                .andExpect(jsonPath("$.watchIntervalMs").value(100))
                .andExpect(jsonPath("$.absoluteTimeMs").value(5000));
    }

    @Test
    void adminGetsBadRequestForOutOfRangeValue() throws Exception {
        mockMvc.perform(put(URL).header("Authorization", bearer("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"outputSizeBytes\":1,\"errorSizeBytes\":4096,\"hardTimePercent\":0.5,\"watchIntervalMs\":100,\"absoluteTimeMs\":5000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminGetsBadRequestForOutOfRangeErrorSize() throws Exception {
        mockMvc.perform(put(URL).header("Authorization", bearer("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"outputSizeBytes\":2048,\"errorSizeBytes\":1,\"hardTimePercent\":0.5,\"watchIntervalMs\":100,\"absoluteTimeMs\":5000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminGetsBadRequestForMissingField() throws Exception {
        mockMvc.perform(put(URL).header("Authorization", bearer("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"outputSizeBytes\":2048}")).andExpect(status().isBadRequest());
    }
}
