package br.com.agroclima.mapper;

import br.com.agroclima.config.RoleCatalog;
import br.com.agroclima.domain.UserEntity;
import br.com.agroclima.dto.UserDtos;

public final class UserMapper {
    private UserMapper() {}

    public static UserDtos.UserResponse toResponse(UserEntity user) {
        String roleKey = user.getRoles().stream().map(role -> role.getName()).sorted().findFirst().orElse("PRODUCER");
        return new UserDtos.UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
            user.getOrganization(), user.getJobTitle(), roleKey, RoleCatalog.label(roleKey),
            user.getStatus() == UserEntity.Status.ACTIVE ? "Ativo" : "Bloqueado",
            user.getCreatedAt(), user.getLastLoginAt());
    }
}
