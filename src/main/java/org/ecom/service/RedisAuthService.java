package org.ecom.service;

import lombok.RequiredArgsConstructor;
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
    private static final long USER_CACHE_TTL = 30;
    private static final long TOKEN_CACHE_TTL = 60;

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
    public void cacheRefreshToken(String refreshToken, String username){
        String key = TOKEN_CACHE_PREFIX + refreshToken;
        redisTemplate.opsForValue().set(key, username, TOKEN_CACHE_TTL, TimeUnit.MINUTES);
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
    public String getUsernameFromRefreshToken(String refreshToken) {
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
}
