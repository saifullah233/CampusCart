package com.campuscart.audit.service;

import com.campuscart.admin.service.AdminAccessService;
import com.campuscart.audit.domain.AuditLog;
import com.campuscart.audit.dto.AuditLogResponse;
import com.campuscart.audit.repository.AuditLogRepository;
import com.campuscart.common.api.PageResponse;
import com.campuscart.common.exception.BusinessRuleException;
import com.campuscart.user.domain.User;
import com.campuscart.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AdminAccessService adminAccessService;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, AdminAccessService adminAccessService,
                           UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.adminAccessService = adminAccessService;
        this.userRepository = userRepository;
    }

    @Transactional
    public void record(User actor, String action, String targetType, UUID targetId, String details) {
        auditLogRepository.save(new AuditLog(actor, action, targetType, targetId, details));
    }

    @Transactional
    public void recordIfPresent(UUID actorId, String action, String targetType, UUID targetId, String details) {
        userRepository.findById(actorId)
                .ifPresent(actor -> record(actor, action, targetType, targetId, details));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(UUID adminId, int page, int size) {
        adminAccessService.requireAdmin(adminId);
        if (page < 0 || size < 1 || size > 50) {
            throw new BusinessRuleException("Page must be non-negative and size must be between 1 and 50.");
        }
        return PageResponse.from(auditLogRepository.findAllByOrderByCreatedAtDesc(
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(log -> new AuditLogResponse(log.getId(), log.getActor().getId(), log.getActor().getEmail(),
                        log.getAction(), log.getTargetType(), log.getTargetId(), log.getDetails(), log.getCreatedAt())));
    }
}
