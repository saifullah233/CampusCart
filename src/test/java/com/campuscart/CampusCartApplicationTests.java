package com.campuscart;

import com.campuscart.support.AbstractMySqlIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Verifies the Spring application context starts with the full Part-2 persistence stack
 * wired: datasource, Flyway migrations applied, and Hibernate schema {@code validate}
 * passing against a real MySQL 8 (see {@link AbstractMySqlIntegrationTest}).
 *
 * <p>A mapping/DDL mismatch (e.g. an entity column absent from a migration) fails
 * context startup, and therefore this test.</p>
 */
class CampusCartApplicationTests extends AbstractMySqlIntegrationTest {

    @Test
    void contextLoads() {
        // Intentionally empty: a failure to build the context fails this test.
    }
}
