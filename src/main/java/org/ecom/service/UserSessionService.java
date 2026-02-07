package org.ecom.service;

import org.ecom.model.User;
import org.ecom.model.UserSession;
import org.ecom.repository.UserSessionRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class UserSessionService {

    private final UserSessionRepository userSessionRepository;

    public UserSessionService(UserSessionRepository userSessionRepository) {
        this.userSessionRepository = userSessionRepository;
    }

    public UserSession createSession(
            User user,
            String sessionId,
            String familyId,
            String refreshToken,
            Instant expiresAt,
            String ipAddress,
            String userAgent
    ) {
        UserSession session = new UserSession();
        session.setUser(user);
        session.setSessionId(sessionId);
        session.setFamilyId(familyId);
        session.setRefreshTokenHash(hashToken(refreshToken));
        session.setExpiresAt(expiresAt);
        session.setRevoked(false);
        session.setCreatedAt(Instant.now());
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        return userSessionRepository.save(session);
    }

    public Optional<UserSession> findBySessionId(String sessionId) {
        return userSessionRepository.findBySessionId(sessionId);
    }

    public boolean isRefreshTokenMatch(UserSession session, String refreshToken) {
        return hashToken(refreshToken).equals(session.getRefreshTokenHash());
    }

    public void revokeSession(String sessionId, String replacedBySessionId) {
        userSessionRepository.revokeSession(sessionId, replacedBySessionId, Instant.now());
    }

    public void revokeFamily(String familyId) {
        userSessionRepository.revokeFamily(familyId, Instant.now());
    }

    public void touchSession(String sessionId) {
        userSessionRepository.touchSession(sessionId, Instant.now());
    }

    public int purgeExpired() {
        return userSessionRepository.deleteExpired(Instant.now());
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
