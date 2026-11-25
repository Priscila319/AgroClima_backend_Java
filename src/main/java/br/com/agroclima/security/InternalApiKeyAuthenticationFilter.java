package br.com.agroclima.security;

import br.com.agroclima.config.AppProperties;
import br.com.agroclima.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;

public class InternalApiKeyAuthenticationFilter extends OncePerRequestFilter {
    private final String expectedKey;
    private final ObjectMapper mapper;

    public InternalApiKeyAuthenticationFilter(AppProperties properties, ObjectMapper mapper) {
        this.expectedKey = properties.security().internalApiKey();
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String providedKey = request.getHeader("X-Internal-Api-Key");
        if (expectedKey == null || expectedKey.length() < 32) {
            writeError(request, response, HttpStatus.SERVICE_UNAVAILABLE,
                "A integração interna não está configurada.");
            return;
        }
        if (providedKey == null || !MessageDigest.isEqual(
            expectedKey.getBytes(StandardCharsets.UTF_8), providedKey.getBytes(StandardCharsets.UTF_8))) {
            writeError(request, response, HttpStatus.UNAUTHORIZED, "Credencial de serviço inválida.");
            return;
        }
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
            "analytics-service", null, AuthorityUtils.createAuthorityList("ROLE_SERVICE"));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response,
                            HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), new ApiError(Instant.now(), status.value(),
            status.getReasonPhrase(), message, request.getRequestURI(), Map.of()));
    }
}
