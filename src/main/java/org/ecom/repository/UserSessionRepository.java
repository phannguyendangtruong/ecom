package org.ecom.repository;

import org.ecom.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findBySessionId(String sessionId);

    @Modifying
    @Query("update UserSession s set s.revoked = true, s.revokedAt = :now where s.familyId = :familyId and s.revoked = false")
    int revokeFamily(@Param("familyId") String familyId, @Param("now") Instant now);

    @Modifying
    @Query("update UserSession s set s.revoked = true, s.revokedAt = :now, s.replacedBySessionId = :replacedBy where s.sessionId = :sessionId and s.revoked = false")
    int revokeSession(@Param("sessionId") String sessionId, @Param("replacedBy") String replacedBy, @Param("now") Instant now);

    @Modifying
    @Query("update UserSession s set s.lastUsedAt = :now where s.sessionId = :sessionId")
    int touchSession(@Param("sessionId") String sessionId, @Param("now") Instant now);

    @Modifying
    @Query("delete from UserSession s where s.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
