package com.campuscart.location.web;

import com.campuscart.common.api.ApiResponse;
import com.campuscart.common.api.PageResponse;
import com.campuscart.location.dto.CityRequest;
import com.campuscart.location.dto.CityResponse;
import com.campuscart.location.service.AdminCityService;
import com.campuscart.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/cities")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCityController {

    private final AdminCityService cityService;

    public AdminCityController(AdminCityService cityService) {
        this.cityService = cityService;
    }

    @GetMapping
    public ApiResponse<PageResponse<CityResponse>> list(@AuthenticationPrincipal AuthenticatedUser principal,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(cityService.list(principal.id(), page, size));
    }

    @GetMapping("/{cityId}")
    public ApiResponse<CityResponse> get(@AuthenticationPrincipal AuthenticatedUser principal,
                                         @PathVariable UUID cityId) {
        return ApiResponse.ok(cityService.get(principal.id(), cityId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CityResponse>> create(@AuthenticationPrincipal AuthenticatedUser principal,
                                                             @Valid @RequestBody CityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(cityService.create(principal.id(), request)));
    }

    @PatchMapping("/{cityId}")
    public ApiResponse<CityResponse> update(@AuthenticationPrincipal AuthenticatedUser principal,
                                             @PathVariable UUID cityId, @Valid @RequestBody CityRequest request) {
        return ApiResponse.ok(cityService.update(principal.id(), cityId, request));
    }

    @PostMapping("/{cityId}/activate")
    public ApiResponse<CityResponse> activate(@AuthenticationPrincipal AuthenticatedUser principal,
                                              @PathVariable UUID cityId) {
        return ApiResponse.ok(cityService.activate(principal.id(), cityId));
    }

    @PostMapping("/{cityId}/deactivate")
    public ApiResponse<CityResponse> deactivate(@AuthenticationPrincipal AuthenticatedUser principal,
                                                @PathVariable UUID cityId) {
        return ApiResponse.ok(cityService.deactivate(principal.id(), cityId));
    }
}
