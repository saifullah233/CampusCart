package com.campuscart.security.otp;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select challenge from OtpChallenge challenge join fetch challenge.user "
            + "where challenge.id = :id")
    Optional<OtpChallenge> findByIdForUpdate(@Param("id") UUID id);

    @Query("select count(challenge) from OtpChallenge challenge "
            + "where challenge.destinationHash = :destinationHash and challenge.createdAt >= :since")
    long countRecentByDestinationHash(@Param("destinationHash") String destinationHash,
                                      @Param("since") Instant since);
}
