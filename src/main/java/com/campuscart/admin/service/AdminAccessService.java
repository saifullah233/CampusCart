package com.campuscart.admin.service;

import com.campuscart.user.domain.User;
import com.campuscart.user.service.UserService;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class AdminAccessService {

    private final UserService userService;

    public AdminAccessService(UserService userService) {
        this.userService = userService;
    }

    public User requireAdmin(UUID userId) {
        User user = userService.requireActive(userId);
        if (!user.getRole().isAdmin()) {
            throw new AccessDeniedException("Administrator access is required.");
        }
        return user;
    }
}
