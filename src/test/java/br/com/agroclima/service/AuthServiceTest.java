package br.com.agroclima.service;

import br.com.agroclima.exception.ApiException;
import br.com.agroclima.repository.UserRepository;
import br.com.agroclima.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    @Mock UserService users;
    @Mock UserRepository userRepository;
    @Mock AuditService audit;
    @InjectMocks AuthService service;

    @Test
    void failedLoginReturnsGenericUnauthorizedAndRecordsAttempt() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("internal detail"));

        ApiException error = assertThrows(ApiException.class, () -> service.login(
            new br.com.agroclima.dto.AuthDtos.LoginRequest("USER@example.test", "wrong-password")));

        assertEquals(401, error.getStatus().value());
        verify(users).recordFailedLogin("user@example.test");
        verify(audit).recordAuthenticationFailure("user@example.test");
    }
}
