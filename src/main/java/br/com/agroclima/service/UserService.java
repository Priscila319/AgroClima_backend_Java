package br.com.agroclima.service;

import br.com.agroclima.config.AppProperties;
import br.com.agroclima.config.RoleCatalog;
import br.com.agroclima.domain.RoleEntity;
import br.com.agroclima.domain.UserEntity;
import br.com.agroclima.dto.UserDtos;
import br.com.agroclima.exception.ApiException;
import br.com.agroclima.mapper.UserMapper;
import br.com.agroclima.repository.RoleRepository;
import br.com.agroclima.repository.UserRepository;
import br.com.agroclima.security.AgroUserPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;
    private final SettingsService settings;
    private final AppProperties properties;

    public UserService(UserRepository users, RoleRepository roles, PasswordEncoder passwordEncoder,
                       AuditService audit, SettingsService settings, AppProperties properties) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
        this.settings = settings;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<UserDtos.UserResponse> list() {
        return users.findAllWithRolesByOrderByCreatedAtDesc().stream().map(UserMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserDtos.UserResponse get(UUID id) { return UserMapper.toResponse(findUser(id)); }

    @Transactional
    public UserDtos.UserResponse registerProducer(UserDtos.CreateUserRequest request, String roleKey,
                                                  boolean publicSignup, AgroUserPrincipal actor) {
        boolean strongPasswords = settings.get().path("security").path("strongPasswords").asBoolean(true);
        PasswordPolicy.require(request.password(), strongPasswords);
        String email = normalizeEmail(request.email());
        if (users.existsByEmailIgnoreCase(email)) throw ApiException.conflict("Já existe uma conta com este e-mail.");
        String assignedRole = publicSignup ? RoleCatalog.PRODUCER : roleKey;
        if (!RoleCatalog.contains(assignedRole)) throw ApiException.badRequest("Papel inválido.");
        RoleEntity role = findRole(assignedRole);
        UserEntity user = new UserEntity(request.fullName(), email, passwordEncoder.encode(request.password()),
            request.phone(), request.organization(), request.jobTitle(), Set.of(role));
        UserEntity saved = users.save(user);
        String details = "Conta criada com papel " + RoleCatalog.label(assignedRole) + ".";
        if (publicSignup) audit.record(saved, "Criou uma conta", "Usuários", details);
        else audit.record(actor, "Cadastrou um usuário", "Usuários", details);
        return UserMapper.toResponse(saved);
    }

    @Transactional
    public UserDtos.UserResponse updateProfile(UUID userId, UserDtos.UpdateProfileRequest request, AgroUserPrincipal actor) {
        UserEntity user = findUser(userId);
        user.updateProfile(request.fullName(), request.phone(), request.organization(), request.jobTitle());
        UserEntity saved = users.save(user);
        audit.record(actor, "Atualizou dados de um usuário", "Usuários", saved.getEmail() + " · perfil atualizado.");
        return UserMapper.toResponse(saved);
    }

    @Transactional
    public UserDtos.UserResponse changeRole(UUID userId, String roleKey, AgroUserPrincipal actor) {
        if (!RoleCatalog.contains(roleKey)) throw ApiException.badRequest("Papel inválido.");
        UserEntity user = findUser(userId);
        ensureAdminNotRemoved(user, roleKey, user.getStatus());
        user.setRoles(Set.of(findRole(roleKey)));
        UserEntity saved = users.save(user);
        audit.record(actor, "Alterou o papel de um usuário", "Permissões e RBAC",
            saved.getEmail() + " agora tem o papel " + RoleCatalog.label(roleKey) + ".");
        return UserMapper.toResponse(saved);
    }

    @Transactional
    public UserDtos.UserResponse changeStatus(UUID userId, UserEntity.Status status, AgroUserPrincipal actor) {
        if (status == null) throw ApiException.badRequest("Informe um status válido.");
        UserEntity user = findUser(userId);
        if (user.getId().equals(actor.id()) && status == UserEntity.Status.BLOCKED) {
            throw ApiException.conflict("Não é permitido bloquear a própria conta administrativa.");
        }
        ensureAdminNotRemoved(user, primaryRole(user), status);
        user.setStatus(status);
        UserEntity saved = users.save(user);
        audit.record(actor, status == UserEntity.Status.ACTIVE ? "Ativou um usuário" : "Bloqueou um usuário",
            "Usuários", saved.getEmail() + " · " + (status == UserEntity.Status.ACTIVE ? "Ativo" : "Bloqueado") + ".");
        return UserMapper.toResponse(saved);
    }

    @Transactional
    public void recordSuccessfulLogin(UUID userId) {
        UserEntity user = findUser(userId);
        user.recordLogin(Instant.now());
        users.save(user);
        audit.record(user, "Iniciou sessão", "Sessão", "Autenticação concluída.");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedLogin(String email) {
        boolean lockAfterFailures = settings.get().path("security").path("lockAfterFailures").asBoolean(true);
        if (!lockAfterFailures) return;
        users.findByEmailIgnoreCase(normalizeEmail(email)).ifPresent(user -> {
            user.recordFailedLogin(Instant.now(), properties.security().maxLoginFailures(), properties.security().lockoutMinutes());
            users.save(user);
        });
    }

    private UserEntity findUser(UUID id) {
        return users.findWithRolesById(id).orElseThrow(() -> ApiException.notFound("Usuário não encontrado."));
    }

    private RoleEntity findRole(String key) {
        return roles.findByNameIgnoreCase(key).orElseThrow(() -> ApiException.badRequest("Papel não encontrado."));
    }

    private String primaryRole(UserEntity user) {
        return user.getRoles().stream().map(RoleEntity::getName).sorted().findFirst().orElse(RoleCatalog.PRODUCER);
    }

    private void ensureAdminNotRemoved(UserEntity target, String requestedRole, UserEntity.Status requestedStatus) {
        boolean currentlyAdmin = target.getRoles().stream().anyMatch(role -> RoleCatalog.ADMIN.equals(role.getName()));
        boolean remainsActiveAdmin = RoleCatalog.ADMIN.equals(requestedRole) && requestedStatus == UserEntity.Status.ACTIVE;
        if (currentlyAdmin && !remainsActiveAdmin
            && users.countUsersInRoleWithStatus(RoleCatalog.ADMIN, UserEntity.Status.ACTIVE) <= 1) {
            throw ApiException.conflict("O sistema precisa manter ao menos um Administrador ativo.");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
