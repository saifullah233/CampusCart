package com.campuscart.security.login;

import com.campuscart.common.exception.LoginRateLimitedException;
import com.campuscart.common.util.Hashing;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginRateLimitService {

    private final LoginRateLimitRepository repository;
    private final LoginRateLimitProperties properties;
    private final Clock clock;
    private final Object monitor = new Object();

    public LoginRateLimitService(LoginRateLimitRepository repository,
                                 LoginRateLimitProperties properties,
                                 Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void ensureAllowed(String normalizedEmail) {
        var now = clock.instant();
        repository.findByIdentityHash(identityHash(normalizedEmail))
                .filter(limit -> limit.isLocked(now))
                .ifPresent(limit -> {
                    throw new LoginRateLimitedException();
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String normalizedEmail) {
        synchronized (monitor) {
            var now = clock.instant();
            LoginRateLimit limit = repository.findByIdentityHashForUpdate(identityHash(normalizedEmail))
                    .orElseGet(() -> new LoginRateLimit(identityHash(normalizedEmail), now));
            limit.recordFailure(now, properties.getWindow(), properties.getMaxFailures(), properties.getLockout());
            repository.save(limit);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(String normalizedEmail) {
        synchronized (monitor) {
            var now = clock.instant();
            repository.findByIdentityHashForUpdate(identityHash(normalizedEmail))
                    .ifPresent(limit -> limit.clearFailures(now));
        }
    }

    private String identityHash(String normalizedEmail) {
        return Hashing.sha256Hex("login:" + normalizedEmail);
    }
}
