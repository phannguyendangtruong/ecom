package org.ecom.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ecom.dto.GoogleLoginRequestDto;
import org.ecom.dto.LoginRequestDto;
import org.ecom.dto.RefreshTokenRequest;
import org.ecom.dto.TokenResponseDto;
import org.ecom.reponse.ApiResponse;
import org.ecom.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponseDto>> login(@RequestBody @Valid LoginRequestDto loginRequest){
        return ResponseEntity.ok(ApiResponse.ok(auth.login(loginRequest)));
    }

    @PostMapping("/refresh_token")
    public ResponseEntity<ApiResponse<TokenResponseDto>> refreshToken(@RequestBody RefreshTokenRequest request){
        return ResponseEntity.ok(ApiResponse.ok(auth.refreshToken(request)));
    }

    @PostMapping("/google/login")
    public ResponseEntity<ApiResponse<TokenResponseDto>> loginWithGoogle(@RequestBody @Valid GoogleLoginRequestDto googleLoginRequestDto){
        return ResponseEntity.ok(ApiResponse.ok(auth.loginWithGoogle(googleLoginRequestDto.getGoogleToken())));
    }
}
