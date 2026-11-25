package br.com.agroclima.config;

import br.com.agroclima.domain.UserEntity;
import br.com.agroclima.repository.RoleRepository;
import br.com.agroclima.repository.UserRepository;
import br.com.agroclima.service.PasswordPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);
    private final AppProperties properties;
    private final RoleRepository roles;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapRunner(AppProperties properties, RoleRepository roles,
                                UserRepository users, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.roles = roles;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String password = properties.security().initialAdminPassword();
        String email = properties.security().initialAdminEmail();
        if (password == null || password.isBlank()) {
            log.warn("Administrador inicial não criado: defina INITIAL_ADMIN_PASSWORD e reinicie a API.");
            return;
        }
        if (!PasswordPolicy.isStrong(password)) {
            throw new IllegalStateException("INITIAL_ADMIN_PASSWORD deve ter 8 a 72 caracteres e conter letra e número.");
        }
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new IllegalStateException("INITIAL_ADMIN_EMAIL deve ser um e-mail válido.");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(normalizedEmail)) {
            log.info("Conta administrativa inicial já existe; nenhuma alteração foi feita.");
            return;
        }
        var adminRole = roles.findByNameIgnoreCase(RoleCatalog.ADMIN)
            .orElseThrow(() -> new IllegalStateException("Role ADMIN não foi carregada pela migration."));
        users.save(new UserEntity("Administrador AgroClima", normalizedEmail, passwordEncoder.encode(password),
            "", "", "Administrador", Set.of(adminRole)));
        log.info("Conta administrativa inicial criada para {}.", normalizedEmail);
    }
}
