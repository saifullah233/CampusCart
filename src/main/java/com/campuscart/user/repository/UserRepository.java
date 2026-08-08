package com.campuscart.user.repository;

import com.campuscart.user.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data-access for {@link User}. Introduced in Part 2 primarily to exercise the entity
 * mapping, FK chain, and auditing contract end-to-end against real MySQL; query methods
 * grow with the user/auth modules.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
}
