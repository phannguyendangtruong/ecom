package org.ecom.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecom.dto.LoginRequestDto;
import org.ecom.dto.RefreshTokenRequest;
import org.ecom.dto.TokenResponseDto;
import org.ecom.exception.BusinessException;
import org.ecom.model.User;
import org.ecom.model.UserSession;
import org.ecom.repository.UserRepository;
import org.ecom.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RedisAuthService redisAuthService;
    private final GoogleOAuthService googleOAuthService;
    private final UserSessionService userSessionService;

    @Transactional
    public TokenResponseDto login(LoginRequestDto loginRequest, String clientIp) {
        return login(loginRequest, clientIp, "unknown");
    }

    @Transactional
    public TokenResponseDto login(LoginRequestDto loginRequest, String clientIp, String userAgent) {
        if (redisAuthService.isLoginLocked(loginRequest.getUsername(), clientIp)) {
            throw new BusinessException("Too many failed attempts. Try again later.", HttpStatus.TOO_MANY_REQUESTS);
        }

        log.info("Authenticating username={}", loginRequest.getUsername());
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (BadCredentialsException e) {
            redisAuthService.recordFailedLogin(loginRequest.getUsername(), clientIp);
            throw new BusinessException("Invalid credentials", HttpStatus.BAD_REQUEST);
        }

        redisAuthService.clearFailedLogin(loginRequest.getUsername(), clientIp);
        return issueSessionTokens(user, clientIp, userAgent, null);
    }

    public TokenResponseDto refreshToken(RefreshTokenRequest request) {
        return refreshToken(request, "unknown", "unknown");
    }

    @Transactional
    public TokenResponseDto refreshToken(RefreshTokenRequest request, String clientIp, String userAgent) {
        if (!redisAuthService.allowRefreshAttempt(clientIp)) {
            throw new BusinessException("Too many refresh requests. Try again later.", HttpStatus.TOO_MANY_REQUESTS);
        }

        String refreshToken = request.getRefreshToken();
        if (!jwtUtil.validate(refreshToken) || !REFRESH_TOKEN_TYPE.equals(jwtUtil.getTokenType(refreshToken))) {
            throw new BusinessException("Invalid refresh token", HttpStatus.BAD_REQUEST);
        }

        String sessionId = jwtUtil.getSessionId(refreshToken);
        String familyId = jwtUtil.getFamilyId(refreshToken);
        String username = jwtUtil.getUsername(refreshToken);
        if (sessionId == null || familyId == null || username == null) {
            throw new BusinessException("Invalid refresh token", HttpStatus.BAD_REQUEST);
        }

        UserSession session = userSessionService.findBySessionId(sessionId)
                .orElseThrow(() -> new BusinessException("Refresh session not found", HttpStatus.BAD_REQUEST));

        if (session.isRevoked()) {
            userSessionService.revokeFamily(session.getFamilyId());
            redisAuthService.revokeRefreshToken(refreshToken);
            throw new BusinessException("Refresh token is revoked", HttpStatus.BAD_REQUEST);
        }

        if (!username.equals(session.getUser().getUsername())) {
            userSessionService.revokeFamily(session.getFamilyId());
            redisAuthService.revokeRefreshToken(refreshToken);
            throw new BusinessException("Invalid refresh token", HttpStatus.BAD_REQUEST);
        }

        if (!userSessionService.isRefreshTokenMatch(session, refreshToken)) {
            userSessionService.revokeFamily(session.getFamilyId());
            redisAuthService.revokeRefreshToken(refreshToken);
            throw new BusinessException("Refresh token reuse detected", HttpStatus.BAD_REQUEST);
        }

        Instant now = Instant.now();
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(now)) {
            userSessionService.revokeSession(session.getSessionId(), null);
            redisAuthService.revokeRefreshToken(refreshToken);
            throw new BusinessException("Refresh token expired", HttpStatus.BAD_REQUEST);
        }

        User user = session.getUser();
        String role = user.getRole().getType();
        String newSessionId = UUID.randomUUID().toString();
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUsername(), role, newSessionId, session.getFamilyId());
        String newAccessToken = jwtUtil.generateToken(user.getUsername(), role);

        userSessionService.revokeSession(session.getSessionId(), newSessionId);
        userSessionService.createSession(
                user,
                newSessionId,
                session.getFamilyId(),
                newRefreshToken,
                jwtUtil.getExpiration(newRefreshToken),
                clientIp,
                userAgent
        );

        redisAuthService.revokeRefreshToken(refreshToken);
        redisAuthService.cacheRefreshToken(newRefreshToken, newSessionId);
        redisAuthService.evictUserDetails(username);

        return new TokenResponseDto(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        logout(request, "unknown");
    }

    @Transactional
    public void logout(RefreshTokenRequest request, String clientIp) {
        String refreshToken = request.getRefreshToken();
        if (!jwtUtil.validate(refreshToken)) {
            return;
        }

        String sessionId = jwtUtil.getSessionId(refreshToken);
        String username = jwtUtil.getUsername(refreshToken);
        if (sessionId != null) {
            userSessionService.revokeSession(sessionId, null);
        }
        redisAuthService.revokeRefreshToken(refreshToken);
        if (username != null) {
            redisAuthService.evictUserDetails(username);
        }
        log.info("Logout completed for clientIp={}", clientIp);
    }

    public TokenResponseDto loginWithGoogle(String googleToken) {
        return loginWithGoogle(googleToken, "unknown", "unknown");
    }

    @Transactional
    public TokenResponseDto loginWithGoogle(String googleToken, String clientIp, String userAgent) {
        try {
            User user = googleOAuthService.verifyGoogleToken(googleToken);
            return issueSessionTokens(user, clientIp, userAgent, null);
        } catch (GeneralSecurityException | IOException e) {
            throw new BusinessException("Invalid Google token", HttpStatus.BAD_REQUEST);
        }
    }

    private TokenResponseDto issueSessionTokens(User user, String clientIp, String userAgent, String familyIdOverride) {
        String role = user.getRole().getType();
        String accessToken = jwtUtil.generateToken(user.getUsername(), role);
        String familyId = familyIdOverride == null ? UUID.randomUUID().toString() : familyIdOverride;
        String sessionId = UUID.randomUUID().toString();
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), role, sessionId, familyId);

        userSessionService.createSession(
                user,
                sessionId,
                familyId,
                refreshToken,
                jwtUtil.getExpiration(refreshToken),
                clientIp,
                userAgent
        );

        redisAuthService.cacheRefreshToken(refreshToken, sessionId);
        redisAuthService.evictUserDetails(user.getUsername());
        return new TokenResponseDto(accessToken, refreshToken);
    }
}
