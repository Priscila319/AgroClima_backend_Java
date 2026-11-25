package br.com.agroclima.mapper;

import br.com.agroclima.config.RoleCatalog;
import br.com.agroclima.domain.PermissionEntity;
import br.com.agroclima.domain.UserEntity;
import br.com.agroclima.dto.UserDtos;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class UserMapper {
    private UserMapper() {}

    public static UserDtos.UserResponse toResponse(UserEntity user) {
        String roleKey = user.getRoles().stream().map(role -> role.getName()).sorted().findFirst().orElse("PRODUCER");
        Set<String> permissionKeys = user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .map(PermissionEntity::getKey)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        return new UserDtos.UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
            user.getOrganization(), user.getJobTitle(), roleKey, RoleCatalog.label(roleKey),
            user.getStatus() == UserEntity.Status.ACTIVE ? "Ativo" : "Bloqueado",
            user.getCreatedAt(), user.getLastLoginAt(), Set.copyOf(permissionKeys));
    }
}
