package br.com.agroclima.security;

import br.com.agroclima.config.AppProperties;
import br.com.agroclima.domain.UserEntity;
import br.com.agroclima.service.SettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtServiceTest {
    @Test
    void signsAndReadsSubjectWithConfiguredSecret() {
        SettingsService settings = mock(SettingsService.class);
        ObjectMapper mapper = new ObjectMapper();
        when(settings.get()).thenReturn(mapper.createObjectNode()
            .set("security", mapper.createObjectNode().put("sessionTimeout", "30")));
        AppProperties properties = new AppProperties(
            new AppProperties.Security("unit-test-signing-key-with-at-least-32-bytes", 60,
                "admin@example.test", "", "", 5, 15),
            new AppProperties.Cors("http://localhost:5173"));
        JwtService service = new JwtService(properties, settings);
        UUID userId = UUID.randomUUID();

        JwtService.IssuedToken token = service.issue("user@example.test", userId.toString());
        AgroUserPrincipal principal = new AgroUserPrincipal(userId, "Test User", "user@example.test",
            "hash", UserEntity.Status.ACTIVE, null, Set.of("PRODUCER"), Set.of());

        assertEquals("user@example.test", service.extractSubject(token.value()));
        assertTrue(service.isValidFor(token.value(), principal));
    }
}
