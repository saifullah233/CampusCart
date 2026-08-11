package com.campuscart.chat.repository;

import com.campuscart.chat.domain.Conversation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByBuyerIdAndSellerIdAndProductId(UUID buyerId, UUID sellerId, UUID productId);

    @Query("select conversation from Conversation conversation "
            + "where conversation.id = :conversationId "
            + "and (conversation.buyer.id = :userId or conversation.seller.id = :userId)")
    Optional<Conversation> findByIdAndParticipant(@Param("conversationId") UUID conversationId,
                                                   @Param("userId") UUID userId);

    @EntityGraph(attributePaths = {"buyer", "seller", "product"})
    @Query("select conversation from Conversation conversation "
            + "where conversation.buyer.id = :userId or conversation.seller.id = :userId "
            + "order by conversation.updatedAt desc")
    Page<Conversation> findByParticipant(@Param("userId") UUID userId, Pageable pageable);
}
