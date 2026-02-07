package org.ecom.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecom.dto.LoginRequestDto;
import org.ecom.dto.RefreshTokenRequest;
import org.ecom.dto.TokenResponseDto;
import org.ecom.exception.BusinessException;
import org.ecom.model.User;
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

import java.io.IOException;
import java.security.GeneralSecurityException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RedisAuthService redisAuthService;
    private final GoogleOAuthService googleOAuthService;

    public TokenResponseDto login(LoginRequestDto loginRequest) {
        log.info("Login request: {}", loginRequest);
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (BadCredentialsException e) {
            throw new BusinessException("Username or password is not correct", HttpStatus.BAD_REQUEST);
        }
        String role = user.getRole().getType();
        String token = jwtUtil.generateToken(user.getUsername(), role);
        String refresToken = jwtUtil.generateRefreshToken(user.getUsername(), role);
        user.setRefreshToken(refresToken);
        userRepository.save(user);
        //cache redis
        redisAuthService.cacheRefreshToken(refresToken, user.getUsername());
        // Invalidate cache cũ của user để force reload từ DB
        redisAuthService.evictUserDetails(user.getUsername());
        return new TokenResponseDto(token, refresToken);
    }

    public TokenResponseDto refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        //check redis first
        if (!redisAuthService.isRefreshTokenValid(refreshToken)) {
            if (!userRepository.findByRefreshToken(refreshToken).isPresent()) {
                throw new BusinessException("Refresh token is not present", HttpStatus.BAD_REQUEST);
            }
        }

        if (!jwtUtil.validate(refreshToken)) {
            throw new BusinessException("Invalid refresh token", HttpStatus.BAD_REQUEST);
        }
        String username = jwtUtil.getUsername(refreshToken);
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        ;
        String role = user.getRole().getType();
        String newAccessToken = jwtUtil.generateToken(username, role);
        String newRefreshToken = jwtUtil.generateRefreshToken(username, role);
        user.setRefreshToken(newRefreshToken);
        userRepository.save(user);

        //revoke old token in redis
        redisAuthService.revokeRefreshToken(refreshToken);

        //cache new token in redis
        redisAuthService.cacheRefreshToken(newRefreshToken, username);
        //invalid user cache
        redisAuthService.evictUserDetails(username);

        return new TokenResponseDto(newAccessToken, newRefreshToken);
    }

    public TokenResponseDto loginWithGoogle(String googleToken) {
        try {
            User user = googleOAuthService.verifyGoogleToken(googleToken);

            String role = user.getRole().getType();
            String token = jwtUtil.generateToken(user.getUsername(), role);
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), role);
            user.setRefreshToken(refreshToken);
            userRepository.save(user);

            // Cache redis
            redisAuthService.cacheRefreshToken(refreshToken, user.getUsername());
            redisAuthService.evictUserDetails(user.getUsername());
            return new TokenResponseDto(token, refreshToken);
        } catch (GeneralSecurityException | IOException e) {
            throw new BusinessException("Invalid Google token: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
