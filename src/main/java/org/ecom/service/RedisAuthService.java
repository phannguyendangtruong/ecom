package org.ecom.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisAuthService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String USER_CACHE_PREFIX = "user:";
    private static final String TOKEN_CACHE_PREFIX = "token:";
    private static final String LOGIN_FAIL_PREFIX = "auth:login:fail:";
    private static final String LOGIN_LOCK_PREFIX = "auth:login:lock:";
    private static final String REFRESH_RATE_PREFIX = "auth:refresh:rate:";
    private static final long USER_CACHE_TTL = 30;
    private static final long TOKEN_CACHE_TTL = 60;
    @Value("${app.security.login.max-failed-attempts:5}")
    private long maxLoginFailedAttempts;
    @Value("${app.security.login.fail-window-minutes:15}")
    private long loginFailWindowMinutes;
    @Value("${app.security.login.lock-minutes:15}")
    private long loginLockMinutes;
    @Value("${app.security.refresh.max-attempts-per-window:30}")
    private long maxRefreshAttemptsPerWindow;
    @Value("${app.security.refresh.window-minutes:15}")
    private long refreshWindowMinutes;

    /**
     * Cache UserDetails theo username
     */
    public void cacheUserDetails(String username, UserDetails userDetails){
        String key = USER_CACHE_PREFIX + username;
        redisTemplate.opsForValue().set(key, userDetails, USER_CACHE_TTL, TimeUnit.MINUTES);
    }

    /**
     * Lấy UserDetails từ cache
     */
    public UserDetails getUserDetailsFromCache(String username){
        String key = USER_CACHE_PREFIX + username;
        return (UserDetails) redisTemplate.opsForValue().get(key);
    }

    /**
     * Xóa UserDetails khỏi cache
     */
    public void evictUserDetails(String username){
        String key = USER_CACHE_PREFIX + username;
        redisTemplate.delete(key);
    }

    /**
     * Cache refresh token để kiểm tra nhanh
     */
    public void cacheRefreshToken(String refreshToken, String sessionId){
        String key = TOKEN_CACHE_PREFIX + refreshToken;
        redisTemplate.opsForValue().set(key, sessionId, TOKEN_CACHE_TTL, TimeUnit.MINUTES);
    }

    /**
     * Kiểm tra refresh token có trong cache không
     */
    public boolean isRefreshTokenValid(String refreshToken) {
        String key = TOKEN_CACHE_PREFIX + refreshToken;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Lấy username từ refresh token cache
     */
    public String getSessionIdFromRefreshToken(String refreshToken) {
        String key = TOKEN_CACHE_PREFIX + refreshToken;
        return (String) redisTemplate.opsForValue().get(key);
    }

    /**
     * Xóa refresh token khỏi cache (revoke)
     */
    public void revokeRefreshToken(String refreshToken) {
        String key = TOKEN_CACHE_PREFIX + refreshToken;
        redisTemplate.delete(key);
    }

    /**
     * Xóa tất cả cache của user (khi logout, đổi password)
     */
    public void evictAllUserCache(String username) {
        evictUserDetails(username);
    }

    public long recordFailedLogin(String username, String clientIp) {
        String key = loginFailKey(username, clientIp);
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(key, loginFailWindowMinutes, TimeUnit.MINUTES);
        }
        if (attempts != null && attempts >= maxLoginFailedAttempts) {
            redisTemplate.opsForValue().set(loginLockKey(username, clientIp), "1", loginLockMinutes, TimeUnit.MINUTES);
        }
        return attempts == null ? 0 : attempts;
    }

    public boolean isLoginLocked(String username, String clientIp) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(loginLockKey(username, clientIp)));
    }

    public void clearFailedLogin(String username, String clientIp) {
        redisTemplate.delete(loginFailKey(username, clientIp));
        redisTemplate.delete(loginLockKey(username, clientIp));
    }

    public boolean allowRefreshAttempt(String clientIp) {
        String key = REFRESH_RATE_PREFIX + safe(clientIp);
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(key, refreshWindowMinutes, TimeUnit.MINUTES);
        }
        return attempts != null && attempts <= maxRefreshAttemptsPerWindow;
    }

    private String loginFailKey(String username, String clientIp) {
        return LOGIN_FAIL_PREFIX + safe(username) + ":" + safe(clientIp);
    }

    private String loginLockKey(String username, String clientIp) {
        return LOGIN_LOCK_PREFIX + safe(username) + ":" + safe(clientIp);
    }

    private String safe(String input) {
        if (input == null || input.isBlank()) {
            return "unknown";
        }
        return input.replace(":", "_");
    }
}
