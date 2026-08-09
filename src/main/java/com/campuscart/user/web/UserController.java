package com.campuscart.user.web;

import com.campuscart.common.api.ApiResponse;
import com.campuscart.security.AuthenticatedUser;
import com.campuscart.user.dto.UpdateProfileRequest;
import com.campuscart.user.dto.UserProfileResponse;
import com.campuscart.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> profile(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(userService.getProfile(principal.id()));
    }

    @PatchMapping("/me")
    public ApiResponse<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateProfile(principal.id(), request));
    }
}
