package com.campuscart.chat.repository;

import com.campuscart.chat.domain.ChatReport;
import com.campuscart.chat.domain.ChatReportStatus;
import java.util.UUID;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatReportRepository extends JpaRepository<ChatReport, UUID> {

    Page<ChatReport> findByStatusOrderByCreatedAtAsc(ChatReportStatus status, Pageable pageable);

    Page<ChatReport> findByStatusInOrderByCreatedAtAsc(Collection<ChatReportStatus> statuses, Pageable pageable);

    Page<ChatReport> findByReportedProductIsNotNullAndStatusInOrderByCreatedAtAsc(
            Collection<ChatReportStatus> statuses, Pageable pageable);

    long countByStatus(ChatReportStatus status);

    long countByStatusIn(Collection<ChatReportStatus> statuses);

    boolean existsByReporterIdAndConversationIdAndMessageIdAndStatusIn(
            UUID reporterId, UUID conversationId, UUID messageId, Collection<ChatReportStatus> statuses);

    boolean existsByReporterIdAndReportedUserIdAndStatusIn(
            UUID reporterId, UUID reportedUserId, Collection<ChatReportStatus> statuses);

    boolean existsByReporterIdAndReportedProductIdAndStatusIn(
            UUID reporterId, UUID reportedProductId, Collection<ChatReportStatus> statuses);
}
