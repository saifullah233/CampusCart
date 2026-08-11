package com.campuscart.college.web;

import com.campuscart.college.dto.CollegeRequest;
import com.campuscart.college.dto.CollegeResponse;
import com.campuscart.college.service.AdminCollegeService;
import com.campuscart.common.api.ApiResponse;
import com.campuscart.common.api.PageResponse;
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
@RequestMapping("/api/v1/admin/colleges")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCollegeController {

    private final AdminCollegeService collegeService;

    public AdminCollegeController(AdminCollegeService collegeService) {
        this.collegeService = collegeService;
    }

    @GetMapping
    public ApiResponse<PageResponse<CollegeResponse>> list(@AuthenticationPrincipal AuthenticatedUser principal,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(collegeService.list(principal.id(), page, size));
    }

    @GetMapping("/{collegeId}")
    public ApiResponse<CollegeResponse> get(@AuthenticationPrincipal AuthenticatedUser principal,
                                            @PathVariable UUID collegeId) {
        return ApiResponse.ok(collegeService.get(principal.id(), collegeId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CollegeResponse>> create(@AuthenticationPrincipal AuthenticatedUser principal,
                                                                @Valid @RequestBody CollegeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(collegeService.create(principal.id(), request)));
    }

    @PatchMapping("/{collegeId}")
    public ApiResponse<CollegeResponse> update(@AuthenticationPrincipal AuthenticatedUser principal,
                                                @PathVariable UUID collegeId,
                                                @Valid @RequestBody CollegeRequest request) {
        return ApiResponse.ok(collegeService.update(principal.id(), collegeId, request));
    }

    @PostMapping("/{collegeId}/activate")
    public ApiResponse<CollegeResponse> activate(@AuthenticationPrincipal AuthenticatedUser principal,
                                                  @PathVariable UUID collegeId) {
        return ApiResponse.ok(collegeService.activate(principal.id(), collegeId));
    }

    @PostMapping("/{collegeId}/deactivate")
    public ApiResponse<CollegeResponse> deactivate(@AuthenticationPrincipal AuthenticatedUser principal,
                                                    @PathVariable UUID collegeId) {
        return ApiResponse.ok(collegeService.deactivate(principal.id(), collegeId));
    }
}
