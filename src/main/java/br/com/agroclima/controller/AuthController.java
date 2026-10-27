package br.com.agroclima.controller;

import br.com.agroclima.dto.AuthDtos;
import br.com.agroclima.dto.UserDtos;
import br.com.agroclima.security.AgroUserPrincipal;
import br.com.agroclima.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) { return auth.login(request); }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDtos.UserResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request) { return auth.register(request); }

    @GetMapping("/me")
    public UserDtos.UserResponse currentUser(@AuthenticationPrincipal AgroUserPrincipal principal) {
        return auth.currentUser(principal);
    }
}
