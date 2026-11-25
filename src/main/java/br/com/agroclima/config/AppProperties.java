package br.com.agroclima.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Security security, Cors cors) {
    public AppProperties {
        if (security == null || cors == null) throw new IllegalStateException("Configuração app.security/app.cors obrigatória.");
        if (security.maxLoginFailures() < 1 || security.maxLoginFailures() > 20) {
            throw new IllegalStateException("MAX_LOGIN_FAILURES deve estar entre 1 e 20.");
        }
        if (security.lockoutMinutes() < 1 || security.lockoutMinutes() > 1440) {
            throw new IllegalStateException("LOGIN_LOCKOUT_MINUTES deve estar entre 1 e 1440.");
        }
    }

    public record Security(String jwtSecret, long tokenTtlMinutes, String initialAdminEmail,
                           String initialAdminPassword, String internalApiKey,
                           int maxLoginFailures, long lockoutMinutes) {}

    public record Cors(String allowedOrigins) {}
}
