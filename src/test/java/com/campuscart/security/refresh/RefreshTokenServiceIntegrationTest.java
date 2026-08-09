package com.campuscart.security.refresh;

import com.campuscart.college.domain.College;
import com.campuscart.location.domain.City;
import com.campuscart.common.exception.InvalidTokenException;
import com.campuscart.support.AbstractMySqlIntegrationTest;
import com.campuscart.user.domain.User;
import com.campuscart.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenServiceIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Test
    @Transactional
    void rotatesTokenAndStoresOnlyItsHash() {
        User user = activeUser();

        IssuedRefreshToken first = refreshTokenService.issueFor(user);
        RefreshToken storedFirst = refreshTokenRepository.findById(first.tokenId()).orElseThrow();

        assertThat(storedFirst.getTokenHash()).isNotEqualTo(first.rawToken());
        assertThat(storedFirst.getTokenHash()).hasSize(64);

        IssuedRefreshToken second = refreshTokenService.rotate(first.rawToken());
        RefreshToken rotatedFirst = refreshTokenRepository.findById(first.tokenId()).orElseThrow();

        assertThat(second.rawToken()).isNotEqualTo(first.rawToken());
        assertThat(second.tokenId()).isNotEqualTo(first.tokenId());
        assertThat(rotatedFirst.getRevokedAt()).isNotNull();
        assertThat(rotatedFirst.getReplacedById()).isEqualTo(second.tokenId());
    }

    @Test
    @Transactional
    void replayingRotatedTokenRevokesTheReplacement() {
        User user = activeUser();
        IssuedRefreshToken first = refreshTokenService.issueFor(user);
        IssuedRefreshToken second = refreshTokenService.rotate(first.rawToken());

        assertThatThrownBy(() -> refreshTokenService.rotate(first.rawToken()))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("The token is invalid or has expired.");

        assertThat(refreshTokenRepository.findById(second.tokenId()).orElseThrow().getRevokedAt())
                .isNotNull();
    }

    private User activeUser() {
        String suffix = UUID.randomUUID().toString();
        City city = new City("Mumbai-" + suffix, "Maharashtra");
        entityManager.persist(city);
        College college = new College("IIT Bombay-" + suffix, city);
        entityManager.persist(college);
        User user = userRepository.saveAndFlush(
                new User("student-" + suffix + "@iitb.ac.in", "Asha Rao", college));
        user.activateAfterVerification();
        userRepository.saveAndFlush(user);
        entityManager.clear();
        return userRepository.findById(user.getId()).orElseThrow();
    }
}
