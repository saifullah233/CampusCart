package com.campuscart.chat.service;

import com.campuscart.chat.domain.BlockedUser;
import com.campuscart.chat.repository.BlockedUserRepository;
import com.campuscart.common.exception.BusinessRuleException;
import com.campuscart.user.domain.User;
import com.campuscart.user.service.UserService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BlockService {

    private final BlockedUserRepository blockRepository;
    private final UserService userService;

    public BlockService(BlockedUserRepository blockRepository, UserService userService) {
        this.blockRepository = blockRepository;
        this.userService = userService;
    }

    @Transactional
    public void block(UUID blockerId, UUID blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new BusinessRuleException("You cannot block your own account.");
        }
        User blocker = userService.requireActive(blockerId);
        User blocked = userService.requireActive(blockedId);
        if (!blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            blockRepository.save(new BlockedUser(blocker, blocked));
        }
    }

    @Transactional
    public void unblock(UUID blockerId, UUID blockedId) {
        userService.requireActive(blockerId);
        blockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    @Transactional(readOnly = true)
    public boolean blockedBetween(UUID firstUserId, UUID secondUserId) {
        return blockRepository.existsBetween(firstUserId, secondUserId);
    }
}
