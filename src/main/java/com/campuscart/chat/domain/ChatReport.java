package com.campuscart.chat.domain;

import com.campuscart.common.domain.BaseEntity;
import com.campuscart.product.domain.Product;
import com.campuscart.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "chat_reports")
public class ChatReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false, foreignKey = @ForeignKey(name = "fk_chat_reports_reporter"))
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id",
            foreignKey = @ForeignKey(name = "fk_chat_reports_conversation"))
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id",
            foreignKey = @ForeignKey(name = "fk_chat_reports_reported_user"))
    private User reportedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_product_id",
            foreignKey = @ForeignKey(name = "fk_chat_reports_reported_product"))
    private Product reportedProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", foreignKey = @ForeignKey(name = "fk_chat_reports_message"))
    private ChatMessage message;

    @Column(name = "reason", nullable = false, length = 80)
    private String reason;

    @Column(name = "details", length = 1000)
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ChatReportStatus status = ChatReportStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by", foreignKey = @ForeignKey(name = "fk_chat_reports_reviewer"))
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected ChatReport() {
        // Required by JPA.
    }

    public ChatReport(User reporter, Conversation conversation, ChatMessage message,
                      String reason, String details) {
        this.reporter = reporter;
        this.conversation = conversation;
        this.message = message;
        this.reason = reason;
        this.details = details;
    }

    public ChatReport(User reporter, User reportedUser, String reason, String details) {
        this.reporter = reporter;
        this.reportedUser = reportedUser;
        this.reason = reason;
        this.details = details;
        this.status = ChatReportStatus.PENDING;
    }

    public ChatReport(User reporter, Product reportedProduct, String reason, String details) {
        this.reporter = reporter;
        this.reportedProduct = reportedProduct;
        this.reason = reason;
        this.details = details;
        this.status = ChatReportStatus.PENDING;
    }

    public void review(User reviewer, ChatReportStatus status, Instant reviewedAt) {
        this.reviewedBy = reviewer;
        this.status = status;
        this.reviewedAt = reviewedAt;
    }

    public User getReporter() { return reporter; }
    public Conversation getConversation() { return conversation; }
    public User getReportedUser() { return reportedUser; }
    public Product getReportedProduct() { return reportedProduct; }
    public ChatMessage getMessage() { return message; }
    public String getReason() { return reason; }
    public String getDetails() { return details; }
    public ChatReportStatus getStatus() { return status; }
    public User getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
}
