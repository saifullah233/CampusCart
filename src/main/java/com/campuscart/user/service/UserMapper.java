package com.campuscart.user.service;

import com.campuscart.user.domain.User;
import com.campuscart.user.dto.UserProfileResponse;
import org.springframework.stereotype.Component;

/** Maps the aggregate to an API-safe profile DTO. */
@Component
public class UserMapper {

    public UserProfileResponse toProfile(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getCity().getId(),
                user.getCity().getName(),
                user.getCity().getState(),
                user.getCollege() == null ? null : user.getCollege().getId(),
                user.getCollege() == null ? null : user.getCollege().getName(),
                user.getAccountType().name(),
                user.getRole().name(),
                user.getStatus().name(),
                user.isEmailVerified(),
                user.isPhoneVerified());
    }
}
