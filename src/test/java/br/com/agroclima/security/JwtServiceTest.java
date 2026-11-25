package br.com.agroclima.security;

import br.com.agroclima.config.AppProperties;
import br.com.agroclima.domain.UserEntity;
import br.com.agroclima.service.SettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private static final String SECRET = "unit-test-signing-key-with-at-least-32-bytes";

    private static JwtService newService(String secret) {
        SettingsService settings = mock(SettingsService.class);
        ObjectMapper mapper = new ObjectMapper();
        when(settings.get()).thenReturn(mapper.createObjectNode());
        return new JwtService(new AppProperties(
            new AppProperties.Security(secret, 60, "admin@example.test", "", "", 5, 15),
            new AppProperties.Cors("http://localhost:5173")), settings);
    }

    private static AgroUserPrincipal principal(UUID id, UserEntity.Status status) {
        return new AgroUserPrincipal(id, "Test User", "user@example.test", "hash", status, null,
            Set.of("PRODUCER"), Set.of());
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService service = newService(SECRET);
        UUID id = UUID.randomUUID();
        String token = service.issue("user@example.test", id.toString()).value();
        char last = token.charAt(token.length() - 3);
        String tampered = token.substring(0, token.length() - 3) + (last == 'A' ? 'B' : 'A') + token.substring(token.length() - 2);

        assertFalse(service.isValidFor(tampered, principal(id, UserEntity.Status.ACTIVE)));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        UUID id = UUID.randomUUID();
        String foreign = newService("another-signing-key-with-at-least-32-bytes!!")
            .issue("user@example.test", id.toString()).value();

        assertFalse(newService(SECRET).isValidFor(foreign, principal(id, UserEntity.Status.ACTIVE)));
    }

    @Test
    void expiredTokenIsRejected() {
        UUID id = UUID.randomUUID();
        Instant past = Instant.now().minusSeconds(3600);
        String expired = Jwts.builder().subject("user@example.test").claim("uid", id.toString())
            .issuedAt(Date.from(past.minusSeconds(60))).expiration(Date.from(past))
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();

        assertFalse(newService(SECRET).isValidFor(expired, principal(id, UserEntity.Status.ACTIVE)));
    }

    @Test
    void validTokenStopsWorkingWhenTheAccountIsBlocked() {
        JwtService service = newService(SECRET);
        UUID id = UUID.randomUUID();
        String token = service.issue("user@example.test", id.toString()).value();

        assertTrue(service.isValidFor(token, principal(id, UserEntity.Status.ACTIVE)));
        assertFalse(service.isValidFor(token, principal(id, UserEntity.Status.BLOCKED)));
    }

    @Test
    void tokenIssuedForAnotherUserIdIsRejected() {
        JwtService service = newService(SECRET);
        String token = service.issue("user@example.test", UUID.randomUUID().toString()).value();

        assertFalse(service.isValidFor(token, principal(UUID.randomUUID(), UserEntity.Status.ACTIVE)));
    }

    @Test
    void shortSecretPreventsStartup() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> newService("curta"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> newService(""));
    }
}
