package org.ecom.service;

import org.ecom.dto.LoginRequestDto;
import org.ecom.dto.RefreshTokenRequest;
import org.ecom.dto.TokenResponseDto;
import org.ecom.exception.BusinessException;
import org.ecom.model.Role;
import org.ecom.model.User;
import org.ecom.model.UserSession;
import org.ecom.repository.UserRepository;
import org.ecom.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private RedisAuthService redisAuthService;
    @Mock
    private GoogleOAuthService googleOAuthService;
    @Mock
    private UserSessionService userSessionService;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginSuccessReturnsTokensAndClearsFailedAttempts() {
        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("alice");
        request.setPassword("Password1!");

        Role role = new Role();
        role.setType("USER");
        User user = new User();
        user.setUsername("alice");
        user.setRole(role);

        when(redisAuthService.isLoginLocked("alice", "127.0.0.1")).thenReturn(false);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        when(jwtUtil.generateToken("alice", "USER")).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(eq("alice"), eq("USER"), anyString(), anyString())).thenReturn("refresh-token");
        when(jwtUtil.getExpiration("refresh-token")).thenReturn(Instant.now().plusSeconds(3600));
        when(userSessionService.createSession(any(), anyString(), anyString(), anyString(), any(), anyString(), anyString()))
                .thenReturn(new UserSession());

        TokenResponseDto response = authService.login(request, "127.0.0.1", "JUnit");

        assertEquals("access-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        verify(redisAuthService).clearFailedLogin("alice", "127.0.0.1");
        verify(redisAuthService, never()).recordFailedLogin(any(), any());
        verify(userSessionService).createSession(eq(user), anyString(), anyString(), eq("refresh-token"), any(), eq("127.0.0.1"), eq("JUnit"));
    }

    @Test
    void loginLockedThrowsTooManyRequests() {
        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("alice");
        request.setPassword("Password1!");
        when(redisAuthService.isLoginLocked("alice", "127.0.0.1")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(request, "127.0.0.1"));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
        verifyNoInteractions(userRepository);
    }

    @Test
    void refreshRateExceededThrowsTooManyRequests() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");
        when(redisAuthService.allowRefreshAttempt("127.0.0.1")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshToken(request, "127.0.0.1", "JUnit"));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
        verifyNoInteractions(userRepository);
    }
}
