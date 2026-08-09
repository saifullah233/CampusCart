package com.campuscart.security;

import com.campuscart.user.domain.User;
import com.campuscart.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Database-backed credential lookup for the future password-authentication adapter.
 *
 * <p>No login endpoint is exposed in this part. Registering the lookup now prevents
 * Spring Boot from creating and logging a generated default user, while keeping all
 * credential verification behind Spring Security's configured password encoder.</p>
 */
@Service
public class CampusCartUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CampusCartUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new UsernameNotFoundException("User credentials are not configured");
        }

        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(user.getRole().authority())
                .disabled(!user.getStatus().canAuthenticate())
                .accountLocked(user.getStatus() == com.campuscart.user.domain.AccountStatus.SUSPENDED)
                .build();
    }
}
