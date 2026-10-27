package br.com.agroclima.dto;

import br.com.agroclima.domain.UserEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class UserDtos {
    private UserDtos() {}

    public record UserResponse(UUID id, String name, String email, String phone, String organization,
                               String jobTitle, String roleKey, String role, String status,
                               Instant createdAt, Instant lastLoginAt) {}

    public record CreateUserRequest(
        @NotBlank @Size(min = 2, max = 160) String fullName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @Size(max = 30) String phone,
        @Size(max = 160) String organization,
        @Size(max = 120) String jobTitle,
        @NotBlank @Pattern(regexp = "PRODUCER|ANALYST|ADMIN") String roleKey
    ) {}

    public record UpdateProfileRequest(
        @NotBlank @Size(min = 2, max = 160) String fullName,
        @Size(max = 30) String phone,
        @Size(max = 160) String organization,
        @Size(max = 120) String jobTitle
    ) {}

    public record ChangeRoleRequest(@NotBlank @Pattern(regexp = "PRODUCER|ANALYST|ADMIN") String roleKey) {}
    public record ChangeStatusRequest(@jakarta.validation.constraints.NotNull UserEntity.Status status) {}
}
