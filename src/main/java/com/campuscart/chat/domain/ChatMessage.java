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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(
        name = "chat_messages",
        indexes = @Index(name = "idx_chat_messages_conversation_created", columnList = "conversation_id,created_at"))
public class ChatMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_messages_conversation"))
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_messages_sender"))
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private ChatMessageType messageType;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_storage_key", length = 512)
    private String imageStorageKey;

    @Column(name = "image_delivery_url", length = 1024)
    private String imageDeliveryUrl;

    @Column(name = "image_content_type", length = 80)
    private String imageContentType;

    @Column(name = "image_size_bytes")
    private Long imageSizeBytes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_product_id", foreignKey = @ForeignKey(name = "fk_chat_messages_product"))
    private Product sharedProduct;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 30)
    private ChatModerationStatus moderationStatus = ChatModerationStatus.CLEAR;

    @Column(name = "read_at")
    private Instant readAt;

    protected ChatMessage() {
        // Required by JPA.
    }

    private ChatMessage(Conversation conversation, User sender, ChatMessageType messageType,
                        String content, Product sharedProduct) {
        this.conversation = conversation;
        this.sender = sender;
        this.messageType = messageType;
        this.content = content;
        this.sharedProduct = sharedProduct;
    }

    public static ChatMessage text(Conversation conversation, User sender, String content) {
        return new ChatMessage(conversation, sender, ChatMessageType.TEXT, content, null);
    }

    public static ChatMessage product(Conversation conversation, User sender, Product product) {
        return new ChatMessage(conversation, sender, ChatMessageType.PRODUCT, null, product);
    }

    public static ChatMessage image(Conversation conversation, User sender, String storageKey,
                                    String deliveryUrl, String contentType, long sizeBytes,
                                    ChatModerationStatus moderationStatus) {
        ChatMessage message = new ChatMessage(conversation, sender, ChatMessageType.IMAGE, null, null);
        message.imageStorageKey = storageKey;
        message.imageDeliveryUrl = deliveryUrl;
        message.imageContentType = contentType;
        message.imageSizeBytes = sizeBytes;
        message.moderationStatus = moderationStatus;
        return message;
    }

    public void markRead(Instant readAt) {
        if (this.readAt == null) {
            this.readAt = readAt;
        }
    }

    public Conversation getConversation() { return conversation; }
    public User getSender() { return sender; }
    public ChatMessageType getMessageType() { return messageType; }
    public String getContent() { return content; }
    public String getImageStorageKey() { return imageStorageKey; }
    public String getImageDeliveryUrl() { return imageDeliveryUrl; }
    public String getImageContentType() { return imageContentType; }
    public Long getImageSizeBytes() { return imageSizeBytes; }
    public Product getSharedProduct() { return sharedProduct; }
    public ChatModerationStatus getModerationStatus() { return moderationStatus; }
    public Instant getReadAt() { return readAt; }
}
