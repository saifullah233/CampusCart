package com.campuscart.security.login;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoginRateLimitRepository extends JpaRepository<LoginRateLimit, UUID> {

    Optional<LoginRateLimit> findByIdentityHash(String identityHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rateLimit from LoginRateLimit rateLimit where rateLimit.identityHash = :identityHash")
    Optional<LoginRateLimit> findByIdentityHashForUpdate(@Param("identityHash") String identityHash);
}
