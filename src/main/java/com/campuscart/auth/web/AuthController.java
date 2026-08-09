package com.campuscart.auth.web;

import com.campuscart.auth.dto.AuthTokenResponse;
import com.campuscart.auth.dto.CommunityRegistrationRequest;
import com.campuscart.auth.dto.LoginRequest;
import com.campuscart.auth.dto.RefreshTokenRequest;
import com.campuscart.auth.dto.RegistrationResponse;
import com.campuscart.auth.dto.StudentRegistrationRequest;
import com.campuscart.auth.service.AuthService;
import com.campuscart.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register/student")
    public ResponseEntity<ApiResponse<RegistrationResponse>> registerStudent(
            @Valid @RequestBody StudentRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration started.", authService.registerStudent(request)));
    }

    @PostMapping("/register/community")
    public ResponseEntity<ApiResponse<RegistrationResponse>> registerCommunity(
            @Valid @RequestBody CommunityRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration started.", authService.registerCommunity(request)));
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.ok("Logged out.", null);
    }
}
