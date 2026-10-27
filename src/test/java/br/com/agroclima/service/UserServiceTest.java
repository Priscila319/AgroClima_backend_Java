package br.com.agroclima.service;

import br.com.agroclima.config.AppProperties;
import br.com.agroclima.domain.RoleEntity;
import br.com.agroclima.dto.UserDtos;
import br.com.agroclima.repository.RoleRepository;
import br.com.agroclima.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuditService audit;
    @Mock SettingsService settings;
    @Mock AppProperties properties;
    @InjectMocks UserService service;

    @Test
    void publicRegistrationCannotChooseAnElevatedRole() {
        var json = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode()
            .set("security", new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode().put("strongPasswords", true));
        when(settings.get()).thenReturn(json);
        RoleEntity producer = new RoleEntity("PRODUCER", "Produtor/Exportador");
        when(roles.findByNameIgnoreCase("PRODUCER")).thenReturn(Optional.of(producer));
        when(passwordEncoder.encode("StrongPass123")).thenReturn("bcrypt-hash");
        when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.registerProducer(new UserDtos.CreateUserRequest("Pessoa Produtora", "producer@example.test",
            "StrongPass123", "", "", "", "ADMIN"), "ADMIN", true, null);

        ArgumentCaptor<br.com.agroclima.domain.UserEntity> saved = ArgumentCaptor.forClass(br.com.agroclima.domain.UserEntity.class);
        verify(users).save(saved.capture());
        assertEquals("producer@example.test", saved.getValue().getEmail());
        assertEquals("PRODUCER", saved.getValue().getRoles().iterator().next().getName());
        verify(roles).findByNameIgnoreCase("PRODUCER");
        verify(roles, never()).findByNameIgnoreCase("ADMIN");
        verify(audit).record(saved.getValue(), "Criou uma conta", "Usuários", "Conta criada com papel Produtor/Exportador.");
    }
}
