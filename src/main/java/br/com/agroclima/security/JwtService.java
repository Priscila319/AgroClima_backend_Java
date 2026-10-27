package br.com.agroclima.security;

import br.com.agroclima.config.AppProperties;
import br.com.agroclima.service.SettingsService;
import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey signingKey;
    private final long configuredTtlMinutes;
    private final SettingsService settings;

    public JwtService(AppProperties properties, SettingsService settings) {
        String secret = properties.security().jwtSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET deve conter pelo menos 32 bytes aleatórios.");
        }
        if (properties.security().tokenTtlMinutes() < 5 || properties.security().tokenTtlMinutes() > 1440) {
            throw new IllegalStateException("JWT_TTL_MINUTES deve estar entre 5 e 1440 minutos.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.configuredTtlMinutes = properties.security().tokenTtlMinutes();
        this.settings = settings;
    }

    public IssuedToken issue(String subject, String userId) {
        long ttl = effectiveTtlMinutes();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl, ChronoUnit.MINUTES);
        String value = Jwts.builder()
            .subject(subject)
            .claim("uid", userId)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(signingKey)
            .compact();
        return new IssuedToken(value, expiresAt);
    }

    public String extractSubject(String token) { return parseClaims(token).getSubject(); }

    public boolean isValidFor(String token, AgroUserPrincipal principal) {
        try {
            Claims claims = parseClaims(token);
            return principal.getUsername().equalsIgnoreCase(claims.getSubject())
                && principal.id().toString().equals(claims.get("uid", String.class))
                && claims.getExpiration() != null
                && claims.getExpiration().after(new Date())
                && principal.isEnabled()
                && principal.isAccountNonLocked();
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    private long effectiveTtlMinutes() {
        JsonNode setting = settings.get().path("security").path("sessionTimeout");
        long requested = setting.asLong(configuredTtlMinutes);
        return requested >= 5 && requested <= 1440 ? requested : configuredTtlMinutes;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }

    public record IssuedToken(String value, Instant expiresAt) {}
}
