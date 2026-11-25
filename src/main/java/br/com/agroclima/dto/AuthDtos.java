package br.com.agroclima.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(@NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(max = 72) String password) {}

    public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, UserDtos.UserResponse user) {}
}
