package org.ecom.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.ecom.config.JwtProperties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    @Test
    void validateAcceptsCurrentAndPreviousSecret() {
        String current = "01234567890123456789012345678901";
        String previous = "abcdefghijklmnopqrstuvwxyz012345";

        JwtProperties properties = new JwtProperties();
        properties.setSecret(current);
        properties.setPreviousSecrets(previous);
        properties.setExpirationMs(3600000L);
        properties.setRefreshExpirationMs(86400000L);
        JwtUtil util = new JwtUtil(properties);

        String currentToken = util.generateToken("alice", "USER");
        assertTrue(util.validate(currentToken));
        assertEquals("alice", util.getUsername(currentToken));

        Date now = new Date();
        String previousToken = Jwts.builder()
                .setSubject("bob")
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + 60000L))
                .signWith(Keys.hmacShaKeyFor(previous.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        assertTrue(util.validate(previousToken));
        assertEquals("bob", util.getUsername(previousToken));
        assertFalse(util.validate("not-a-jwt"));
    }
}
