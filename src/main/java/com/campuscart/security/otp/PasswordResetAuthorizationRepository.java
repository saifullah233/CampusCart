package com.campuscart.security.otp;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetAuthorizationRepository extends JpaRepository<PasswordResetAuthorization, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from PasswordResetAuthorization a join fetch a.user where a.tokenHash = :tokenHash")
    Optional<PasswordResetAuthorization> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
