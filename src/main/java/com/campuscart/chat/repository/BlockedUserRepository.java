package com.campuscart.chat.repository;

import com.campuscart.chat.domain.BlockedUser;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlockedUserRepository extends JpaRepository<BlockedUser, UUID> {

    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    void deleteByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    @Query("select count(block) > 0 from BlockedUser block "
            + "where (block.blocker.id = :firstUserId and block.blocked.id = :secondUserId) "
            + "or (block.blocker.id = :secondUserId and block.blocked.id = :firstUserId)")
    boolean existsBetween(@Param("firstUserId") UUID firstUserId,
                          @Param("secondUserId") UUID secondUserId);
}
