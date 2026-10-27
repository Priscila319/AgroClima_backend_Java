package br.com.agroclima.service;

import br.com.agroclima.config.RoleCatalog;
import br.com.agroclima.domain.UserEntity;
import br.com.agroclima.dto.AuthDtos;
import br.com.agroclima.dto.UserDtos;
import br.com.agroclima.exception.ApiException;
import br.com.agroclima.mapper.UserMapper;
import br.com.agroclima.repository.UserRepository;
import br.com.agroclima.security.AgroUserPrincipal;
import br.com.agroclima.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService users;
    private final UserRepository userRepository;
    private final AuditService audit;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService,
                       UserService users, UserRepository userRepository, AuditService audit) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.users = users;
        this.userRepository = userRepository;
        this.audit = audit;
    }

    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        Authentication authentication;
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            users.recordFailedLogin(email);
            audit.recordAuthenticationFailure(email);
            throw ApiException.unauthorized("E-mail ou senha inválidos, ou conta indisponível.");
        }
        try {
            authentication = authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                email, request.password()));
        } catch (AuthenticationException exception) {
            users.recordFailedLogin(email);
            audit.recordAuthenticationFailure(email);
            throw ApiException.unauthorized("E-mail ou senha inválidos, ou conta indisponível.");
        }
        if (!(authentication.getPrincipal() instanceof AgroUserPrincipal principal)) {
            throw ApiException.unauthorized("E-mail ou senha inválidos, ou conta indisponível.");
        }
        users.recordSuccessfulLogin(principal.id());
        UserEntity current = userRepository.findWithRolesById(principal.id())
            .orElseThrow(() -> ApiException.unauthorized("Conta indisponível."));
        JwtService.IssuedToken token = jwtService.issue(current.getEmail(), current.getId().toString());
        return new AuthDtos.AuthResponse(token.value(), "Bearer", token.expiresAt(), UserMapper.toResponse(current));
    }

    @Transactional
    public UserDtos.UserResponse register(AuthDtos.RegisterRequest request) {
        UserDtos.CreateUserRequest safeRequest = new UserDtos.CreateUserRequest(request.fullName(), request.email(),
            request.password(), request.phone(), request.organization(), request.jobTitle(), RoleCatalog.PRODUCER);
        return users.registerProducer(safeRequest, RoleCatalog.PRODUCER, true, null);
    }

    @Transactional(readOnly = true)
    public UserDtos.UserResponse currentUser(AgroUserPrincipal principal) {
        UserEntity user = userRepository.findWithRolesById(principal.id())
            .orElseThrow(() -> ApiException.unauthorized("Conta indisponível."));
        return UserMapper.toResponse(user);
    }
}
