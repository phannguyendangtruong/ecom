package org.ecom.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.RequiredArgsConstructor;
import org.ecom.config.JwtProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    public String generateToken(String username, String role){
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getExpirationMs());
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .claim("type", "access")
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(currentSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String username, String role){
        return generateRefreshToken(username, role, UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    public String generateRefreshToken(String username, String role, String sessionId, String familyId){
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getRefreshExpirationMs());
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .claim("type","refresh")
                .claim("sid", sessionId)
                .claim("fid", familyId)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(currentSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsername(String token){
        return parseToken(token).map(claims -> claims.getBody().getSubject()).orElse(null);
    }

    public boolean validate(String token){
        return parseToken(token).isPresent();
    }

    public String getTokenType(String token) {
        return getStringClaim(token, "type");
    }

    public String getSessionId(String token) {
        return getStringClaim(token, "sid");
    }

    public String getFamilyId(String token) {
        return getStringClaim(token, "fid");
    }

    public Instant getExpiration(String token) {
        return parseToken(token)
                .map(claims -> claims.getBody().getExpiration().toInstant())
                .orElse(null);
    }

    private java.util.Optional<io.jsonwebtoken.Jws<io.jsonwebtoken.Claims>> parseToken(String token) {
        for (SecretKey key : validationKeys()) {
            try {
                return java.util.Optional.of(
                        Jwts.parserBuilder()
                                .setSigningKey(key)
                                .build()
                                .parseClaimsJws(token)
                );
            } catch (SecurityException | io.jsonwebtoken.MalformedJwtException |
                     io.jsonwebtoken.ExpiredJwtException | io.jsonwebtoken.UnsupportedJwtException |
                     IllegalArgumentException ignored) {
                // try next secret
            }
        }
        return java.util.Optional.empty();
    }

    private String getStringClaim(String token, String claimKey) {
        return parseToken(token)
                .map(claims -> claims.getBody().get(claimKey, String.class))
                .orElse(null);
    }

    private SecretKey currentSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    private List<SecretKey> validationKeys() {
        String current = jwtProperties.getSecret();
        String previous = jwtProperties.getPreviousSecrets() == null ? "" : jwtProperties.getPreviousSecrets();
        List<SecretKey> keys = Arrays.stream((current + "," + previous).split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> Keys.hmacShaKeyFor(s.getBytes(StandardCharsets.UTF_8)))
                .collect(Collectors.toList());
        if (keys.isEmpty()) {
            throw new IllegalStateException("No JWT signing keys configured");
        }
        return keys;
    }
}
