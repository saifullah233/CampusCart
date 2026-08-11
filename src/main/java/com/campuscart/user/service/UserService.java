package com.campuscart.user.service;

import com.campuscart.common.exception.AccountNotActiveException;
import com.campuscart.common.exception.ResourceNotFoundException;
import com.campuscart.notification.domain.NotificationType;
import com.campuscart.notification.service.NotificationService;
import com.campuscart.user.domain.User;
import com.campuscart.user.dto.UpdateProfileRequest;
import com.campuscart.user.dto.UserProfileResponse;
import com.campuscart.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public UserService(UserRepository userRepository, UserMapper userMapper,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public User requireActive(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        if (!user.getStatus().canAuthenticate()) {
            throw new AccountNotActiveException();
        }
        return user;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        return userMapper.toProfile(requireActive(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = requireActive(userId);
        user.setFullName(request.fullName().trim());
        User saved = userRepository.save(user);
        notificationService.create(userId, NotificationType.ACCOUNT_EVENT, "Account updated",
                "Your account profile was updated.", "{\"event\":\"PROFILE_UPDATED\"}");
        return userMapper.toProfile(saved);
    }
}
