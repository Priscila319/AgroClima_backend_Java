package br.com.agroclima.service;

import br.com.agroclima.config.RoleCatalog;
import br.com.agroclima.domain.PermissionEntity;
import br.com.agroclima.domain.RoleEntity;
import br.com.agroclima.dto.RoleDtos;
import br.com.agroclima.exception.ApiException;
import br.com.agroclima.repository.PermissionRepository;
import br.com.agroclima.repository.RoleRepository;
import br.com.agroclima.security.AgroUserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoleService {
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final AuditService audit;

    public RoleService(RoleRepository roles, PermissionRepository permissions, AuditService audit) {
        this.roles = roles;
        this.permissions = permissions;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<RoleDtos.RoleResponse> listRoles() {
        return roles.findAllByOrderByNameAsc().stream().map(role -> new RoleDtos.RoleResponse(
            role.getName(), RoleCatalog.label(role.getName()), role.getDescription(),
            role.getPermissions().stream().map(PermissionEntity::getKey).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
        )).toList();
    }

    @Transactional(readOnly = true)
    public List<RoleDtos.PermissionResponse> listPermissions() {
        return permissions.findAll().stream().sorted(java.util.Comparator.comparing(PermissionEntity::getKey))
            .map(permission -> new RoleDtos.PermissionResponse(permission.getKey(), permission.getDescription(), area(permission.getKey())))
            .toList();
    }

    @Transactional
    public RoleDtos.RoleResponse setPermissions(String roleKey, List<String> permissionKeys, AgroUserPrincipal actor) {
        if (!RoleCatalog.contains(roleKey)) throw ApiException.notFound("Papel não encontrado.");
        if (RoleCatalog.ADMIN.equals(roleKey)) throw ApiException.conflict("As permissões do Administrador não podem ser reduzidas.");
        RoleEntity role = roles.findByNameIgnoreCase(roleKey)
            .orElseThrow(() -> ApiException.notFound("Papel não encontrado."));
        List<PermissionEntity> found = permissions.findAllByKeyIn(permissionKeys.stream().distinct().toList());
        Set<String> known = found.stream().map(PermissionEntity::getKey).collect(java.util.stream.Collectors.toSet());
        Set<String> requested = new LinkedHashSet<>(permissionKeys);
        if (!known.containsAll(requested)) throw ApiException.badRequest("Uma ou mais permissões não existem.");
        role.setPermissions(new LinkedHashSet<>(found));
        RoleEntity saved = roles.save(role);
        audit.record(actor, "Atualizou permissões de um papel", "Permissões e RBAC",
            "Papel " + RoleCatalog.label(roleKey) + " atualizado.");
        return new RoleDtos.RoleResponse(saved.getName(), RoleCatalog.label(saved.getName()), saved.getDescription(),
            saved.getPermissions().stream().map(PermissionEntity::getKey).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));
    }

    private String area(String key) {
        if (key.startsWith("plantations.")) return "Plantações";
        if (key.startsWith("users.")) return "Usuários";
        if (key.startsWith("settings.")) return "Configurações";
        if (key.startsWith("logs.")) return "Logs do Sistema";
        if (key.startsWith("security.")) return "Segurança";
        if (key.startsWith("integrations.")) return "Integrações";
        if (key.startsWith("reports.")) return "Relatórios";
        if (key.startsWith("analytics.")) return "Análises";
        if (key.startsWith("monitoring.")) return "Monitoramento";
        if (key.startsWith("design.")) return "Design System";
        return "Painéis";
    }
}
