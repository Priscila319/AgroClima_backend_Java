package br.com.agroclima.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(@NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(max = 72) String password) {}

    /** Registro público sempre cria Produtor; o cliente não pode escolher um papel privilegiado. */
    public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 160) String fullName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @Size(max = 30) String phone,
        @Size(max = 160) String organization,
        @Size(max = 120) String jobTitle
    ) {}

    public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, UserDtos.UserResponse user) {}
}
