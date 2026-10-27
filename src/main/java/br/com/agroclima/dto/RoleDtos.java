package br.com.agroclima.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;

public final class RoleDtos {
    private RoleDtos() {}

    public record RoleResponse(String key, String label, String description, Set<String> permissionKeys) {}
    public record PermissionResponse(String key, String label, String area) {}
    public record UpdatePermissionsRequest(@NotNull List<String> permissionKeys) {}
}
