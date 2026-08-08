package com.campuscart.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuscart.catalog.domain.Category;
import com.campuscart.college.domain.College;
import com.campuscart.college.domain.CollegeEmailDomain;
import com.campuscart.location.domain.City;
import com.campuscart.support.AbstractMySqlIntegrationTest;
import com.campuscart.user.domain.User;
import com.campuscart.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end checks that the Flyway schema and the JPA mappings agree, exercised
 * against a real MySQL 8. Complements the context-load test by asserting the schema
 * contents and a full persistence round-trip (UUID identity, auditing, FK graph).
 */
class SchemaAndPersistenceIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayAppliedEveryMigration() {
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class);
        assertThat(applied).isGreaterThanOrEqualTo(3);
    }

    @Test
    void createsEveryPart2Table() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()",
                String.class);
        assertThat(tables).contains(
                "cities", "colleges", "college_email_domains", "users", "categories");
    }

    @Test
    @Transactional
    void persistsUserGraphWithUuidIdentityAndAuditing() {
        City city = new City("Mumbai", "Maharashtra");
        entityManager.persist(city);
        College college = new College("IIT Bombay", city);
        entityManager.persist(college);
        CollegeEmailDomain domain = new CollegeEmailDomain("iitb.ac.in", college);
        entityManager.persist(domain);

        User saved = userRepository.save(new User("student@iitb.ac.in", "Asha Rao", college));
        entityManager.flush();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getVersion()).isZero();

        entityManager.clear();

        User reloaded = userRepository.findByEmail("student@iitb.ac.in").orElseThrow();
        assertThat(reloaded.getId()).isEqualTo(saved.getId());
        assertThat(reloaded.getCollege().getName()).isEqualTo("IIT Bombay");
        assertThat(reloaded.getCollege().getCity().getState()).isEqualTo("Maharashtra");
    }

    @Test
    @Transactional
    void persistsCategoryWithAuditing() {
        Category category = new Category("Books", "books");
        entityManager.persist(category);
        entityManager.flush();

        assertThat(category.getId()).isNotNull();
        assertThat(category.getCreatedAt()).isNotNull();
        assertThat(category.getVersion()).isZero();
    }
}
