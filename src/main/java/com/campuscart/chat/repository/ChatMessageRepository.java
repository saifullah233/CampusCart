package com.campuscart.chat.repository;

import com.campuscart.chat.domain.ChatMessage;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    Page<ChatMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId, Pageable pageable);

    @Query("select message from ChatMessage message "
            + "where message.conversation.id = :conversationId "
            + "and message.sender.id <> :userId and message.readAt is null")
    List<ChatMessage> findUnreadForRecipient(@Param("conversationId") UUID conversationId,
                                             @Param("userId") UUID userId);

    @Query("select count(message) from ChatMessage message "
            + "where message.conversation.id = :conversationId "
            + "and message.sender.id <> :userId and message.readAt is null")
    long countUnreadForRecipient(@Param("conversationId") UUID conversationId,
                                  @Param("userId") UUID userId);

    /**
     * Batched unread counts for a page of conversations, so a conversation listing does
     * not run one count query per row. Conversations with no unread messages are simply
     * absent from the result.
     */
    @Query("select message.conversation.id as conversationId, count(message) as unread "
            + "from ChatMessage message "
            + "where message.conversation.id in :conversationIds "
            + "and message.sender.id <> :userId and message.readAt is null "
            + "group by message.conversation.id")
    List<ConversationUnreadCount> countUnreadForRecipientByConversationIds(
            @Param("conversationIds") Collection<UUID> conversationIds, @Param("userId") UUID userId);

    boolean existsByIdAndConversationId(UUID messageId, UUID conversationId);

    long countByCreatedAtAfter(java.time.Instant createdAt);

    /** Projection for {@link #countUnreadForRecipientByConversationIds}. */
    interface ConversationUnreadCount {
        UUID getConversationId();

        long getUnread();
    }
}
