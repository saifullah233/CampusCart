package com.campuscart.user.repository;

import com.campuscart.user.domain.User;
import com.campuscart.user.domain.AccountStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data-access for {@link User}. Introduced in Part 2 primarily to exercise the entity
 * mapping, FK chain, and auditing contract end-to-end against real MySQL; query methods
 * grow with the user/auth modules.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    List<User> findByStatus(AccountStatus status);

    long countByStatus(AccountStatus status);

    long countByCreatedAtAfter(java.time.Instant createdAt);

    @EntityGraph(attributePaths = {"city", "college"})
    @Query("select user from User user where (:status is null or user.status = :status) "
            + "and (:query is null or lower(user.email) like lower(concat('%', :query, '%')) "
            + "or lower(user.fullName) like lower(concat('%', :query, '%')))" )
    Page<User> search(@Param("query") String query, @Param("status") AccountStatus status, Pageable pageable);
}
