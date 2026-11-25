package br.com.agroclima.service;

import br.com.agroclima.config.AppProperties;
import br.com.agroclima.domain.RoleEntity;
import br.com.agroclima.domain.UserEntity;
import br.com.agroclima.dto.UserDtos;
import br.com.agroclima.exception.ApiException;
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
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    private void allowStrongPasswordPolicy() {
        var json = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode()
            .set("security", new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode().put("strongPasswords", true));
        when(settings.get()).thenReturn(json);
    }

    @Test
    void adminCreatesUserWithChosenRoleAndNormalizedPhone() {
        allowStrongPasswordPolicy();
        RoleEntity analyst = new RoleEntity("ANALYST", "Analista de Dados");
        when(roles.findByNameIgnoreCase("ANALYST")).thenReturn(Optional.of(analyst));
        when(passwordEncoder.encode("StrongPass123")).thenReturn("bcrypt-hash");
        when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(new UserDtos.CreateUserRequest("Pessoa Analista", "analyst@example.test",
            "StrongPass123", "(81) 99999-8888", "", "", "ANALYST"), null);

        ArgumentCaptor<UserEntity> saved = ArgumentCaptor.forClass(UserEntity.class);
        verify(users).save(saved.capture());
        assertEquals("analyst@example.test", saved.getValue().getEmail());
        assertEquals("ANALYST", saved.getValue().getRoles().iterator().next().getName());
        assertEquals("5581999998888", saved.getValue().getPhone());
        verify(audit).record((br.com.agroclima.security.AgroUserPrincipal) null, "Cadastrou um usuário", "Usuários",
            "Conta criada com papel Analista de Dados.");
    }

    @Test
    void createRejectsInvalidPhoneBeforeSaving() {
        allowStrongPasswordPolicy();

        ApiException error = assertThrows(ApiException.class, () -> service.create(new UserDtos.CreateUserRequest(
            "Pessoa Produtora", "producer@example.test", "StrongPass123", "(00) 99999-8888", "", "", "PRODUCER"), null));

        assertEquals(400, error.getStatus().value());
        assertTrue(error.getMessage().contains("DDD"));
        verify(users, never()).save(any());
    }

    @Test
    void createRejectsWeakPasswordWhenPolicyRequiresStrength() {
        allowStrongPasswordPolicy();

        assertThrows(ApiException.class, () -> service.create(new UserDtos.CreateUserRequest(
            "Pessoa Produtora", "producer@example.test", "onlyletters", "(81) 3333-4444", "", "", "PRODUCER"), null));

        verify(users, never()).save(any());
    }

    @Test
    void updateProfileLoadsAndNormalizesExistingPhone() {
        UUID id = UUID.randomUUID();
        RoleEntity producer = new RoleEntity("PRODUCER", "Produtor/Exportador");
        UserEntity existing = new UserEntity("Pessoa Produtora", "producer@example.test", "hash",
            "558133334444", "", "", Set.of(producer));
        when(users.findWithRolesById(id)).thenReturn(Optional.of(existing));
        when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateProfile(id, new UserDtos.UpdateProfileRequest("Pessoa Produtora", "(81) 99999-8888", "", ""), null);

        assertEquals("5581999998888", existing.getPhone());
    }

    @Test
    void updateProfileRejectsInvalidPhone() {
        UUID id = UUID.randomUUID();
        RoleEntity producer = new RoleEntity("PRODUCER", "Produtor/Exportador");
        UserEntity existing = new UserEntity("Pessoa Produtora", "producer@example.test", "hash",
            "558133334444", "", "", Set.of(producer));
        when(users.findWithRolesById(id)).thenReturn(Optional.of(existing));

        assertThrows(ApiException.class, () -> service.updateProfile(id,
            new UserDtos.UpdateProfileRequest("Pessoa Produtora", "(81) 9999-888", "", ""), null));

        assertEquals("558133334444", existing.getPhone());
        verify(users, never()).save(any());
    }
}
