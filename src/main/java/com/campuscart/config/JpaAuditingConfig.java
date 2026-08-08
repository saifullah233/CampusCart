package com.campuscart.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA auditing so {@code @CreatedDate} / {@code @LastModifiedDate}
 * on {@link com.campuscart.common.domain.BaseEntity} are populated automatically.
 *
 * <p>No {@code AuditorAware} is registered yet: entities track <em>when</em> rows change,
 * not <em>who</em> changed them. The actor dimension is added with the authentication
 * module in a later part.</p>
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
