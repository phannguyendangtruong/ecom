package org.ecom.controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecom.dto.GoogleLoginRequestDto;
import org.ecom.dto.LoginRequestDto;
import org.ecom.dto.RefreshTokenRequest;
import org.ecom.dto.TokenResponseDto;
import org.ecom.response.ApiResponse;
import org.ecom.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/auth", "/api/v1/auth"})
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and token management APIs")
public class AuthController {
    private final AuthService auth;

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate with username/password and issue access + refresh tokens")
    public ResponseEntity<ApiResponse<TokenResponseDto>> login(@RequestBody @Valid LoginRequestDto loginRequest, HttpServletRequest request){
        log.info("Login attempt for username={}", loginRequest.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(auth.login(loginRequest, extractClientIp(request), extractUserAgent(request))));
    }

    @PostMapping("/refresh_token")
    @Operation(summary = "Refresh token", description = "Rotate refresh token and issue a new access token")
    public ResponseEntity<ApiResponse<TokenResponseDto>> refreshToken(@RequestBody @Valid RefreshTokenRequest request, HttpServletRequest httpRequest){
        log.info("Refresh token attempt");
        return ResponseEntity.ok(ApiResponse.ok(auth.refreshToken(
                request,
                extractClientIp(httpRequest),
                extractUserAgent(httpRequest)
        )));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoke current refresh session token")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody @Valid RefreshTokenRequest request, HttpServletRequest httpRequest){
        log.info("Logout attempt");
        auth.logout(request, extractClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.okMessage("Logged out"));
    }

    @PostMapping("/google/login")
    @Operation(summary = "Google login", description = "Authenticate with Google token and issue access + refresh tokens")
    public ResponseEntity<ApiResponse<TokenResponseDto>> loginWithGoogle(
            @RequestBody @Valid GoogleLoginRequestDto googleLoginRequestDto,
            HttpServletRequest request
    ){
        log.info("Google login attempt");
        return ResponseEntity.ok(ApiResponse.ok(auth.loginWithGoogle(
                googleLoginRequestDto.getGoogleToken(),
                extractClientIp(request),
                extractUserAgent(request)
        )));
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return (userAgent == null || userAgent.isBlank()) ? "unknown" : userAgent;
    }
}
