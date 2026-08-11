package com.campuscart.user.service;

import com.campuscart.admin.service.AdminAccessService;
import com.campuscart.audit.service.AuditLogService;
import com.campuscart.common.api.PageResponse;
import com.campuscart.common.exception.BusinessRuleException;
import com.campuscart.common.exception.ResourceNotFoundException;
import com.campuscart.user.domain.AccountStatus;
import com.campuscart.user.domain.User;
import com.campuscart.user.dto.AdminUserResponse;
import com.campuscart.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final AdminAccessService adminAccessService;
    private final AuditLogService auditLogService;

    public AdminUserService(UserRepository userRepository, AdminAccessService adminAccessService,
                            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.adminAccessService = adminAccessService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> list(UUID adminId, String query, AccountStatus status, int page, int size) {
        adminAccessService.requireAdmin(adminId);
        return PageResponse.from(userRepository.search(normalizeQuery(query), status, pageRequest(page, size))
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public AdminUserResponse get(UUID adminId, UUID userId) {
        adminAccessService.requireAdmin(adminId);
        return toResponse(require(userId));
    }

    @Transactional
    public AdminUserResponse suspend(UUID adminId, UUID userId) {
        User admin = adminAccessService.requireAdmin(adminId);
        if (adminId.equals(userId)) {
            throw new BusinessRuleException("Administrators cannot suspend their own account.");
        }
        User user = require(userId);
        if (user.getStatus() == AccountStatus.SUSPENDED) {
            throw new BusinessRuleException("The account is already suspended.");
        }
        user.suspend();
        auditLogService.record(admin, "USER_SUSPENDED", "USER", userId, "Account suspended by administrator.");
        return toResponse(user);
    }

    @Transactional
    public AdminUserResponse activate(UUID adminId, UUID userId) {
        User admin = adminAccessService.requireAdmin(adminId);
        User user = require(userId);
        if (user.getStatus() != AccountStatus.SUSPENDED) {
            throw new BusinessRuleException("Only suspended accounts can be activated by an administrator.");
        }
        user.reactivate();
        auditLogService.record(admin, "USER_ACTIVATED", "USER", userId, "Account activated by administrator.");
        return toResponse(user);
    }

    private User require(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    private PageRequest pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new BusinessRuleException("Page must be non-negative and size must be between 1 and 50.");
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private String normalizeQuery(String query) {
        return query == null || query.isBlank() ? null : query.trim();
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getPhoneNumber(),
                user.getRole().name(), user.getStatus().name(), user.getAccountType().name(), user.getCity().getId(),
                user.getCity().getName(), user.getCollege() == null ? null : user.getCollege().getId(),
                user.getCollege() == null ? null : user.getCollege().getName(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
