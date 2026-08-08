package com.campuscart.location.domain;

import com.campuscart.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A city in which one or more colleges operate.
 *
 * <p>Reference data shared across the platform. A city is uniquely identified by its
 * name within a state, so the same city name may legitimately recur across different
 * states. {@code state} is required specifically so that uniqueness is deterministic
 * (a nullable component would let MySQL treat rows as distinct on NULL).</p>
 */
@Entity
@Table(
        name = "cities",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_cities_name_state",
                columnNames = {"name", "state"}))
public class City extends BaseEntity {

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "state", nullable = false, length = 120)
    private String state;

    protected City() {
        // Required by JPA.
    }

    public City(String name, String state) {
        this.name = name;
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
