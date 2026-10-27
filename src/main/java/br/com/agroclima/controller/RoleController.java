package br.com.agroclima.controller;

import br.com.agroclima.dto.RoleDtos;
import br.com.agroclima.security.AgroUserPrincipal;
import br.com.agroclima.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAuthority('rbac.manage')")
public class RoleController {
    private final RoleService roles;

    public RoleController(RoleService roles) { this.roles = roles; }

    @GetMapping("/roles")
    public List<RoleDtos.RoleResponse> roles() { return roles.listRoles(); }

    @GetMapping("/permissions")
    public List<RoleDtos.PermissionResponse> permissions() { return roles.listPermissions(); }

    @PatchMapping("/roles/{roleKey}/permissions")
    public RoleDtos.RoleResponse update(@PathVariable String roleKey,
                                        @Valid @RequestBody RoleDtos.UpdatePermissionsRequest request,
                                        @AuthenticationPrincipal AgroUserPrincipal actor) {
        return roles.setPermissions(roleKey.toUpperCase(java.util.Locale.ROOT), request.permissionKeys(), actor);
    }
}
