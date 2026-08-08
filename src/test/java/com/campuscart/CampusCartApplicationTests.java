package com.campuscart;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies the Spring application context starts with the Part-1 foundation wired.
 *
 * <p>No datasource/security/redis autoconfiguration is on the classpath yet, so this
 * loads without external infrastructure.</p>
 */
@SpringBootTest
class CampusCartApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty: a failure to build the context fails this test.
    }
}
