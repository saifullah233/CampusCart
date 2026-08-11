package com.campuscart.chat.web;

import com.campuscart.chat.service.BlockService;
import com.campuscart.common.api.ApiResponse;
import com.campuscart.security.AuthenticatedUser;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/blocks")
public class BlockController {

    private final BlockService blockService;

    public BlockController(BlockService blockService) {
        this.blockService = blockService;
    }

    @PostMapping("/{userId}")
    public ApiResponse<Void> block(@AuthenticationPrincipal AuthenticatedUser principal,
                                   @PathVariable UUID userId) {
        blockService.block(principal.id(), userId);
        return ApiResponse.ok("User blocked.", null);
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> unblock(@AuthenticationPrincipal AuthenticatedUser principal,
                                     @PathVariable UUID userId) {
        blockService.unblock(principal.id(), userId);
        return ApiResponse.ok("User unblocked.", null);
    }
}
