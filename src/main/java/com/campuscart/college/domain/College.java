package com.campuscart.college.domain;

import com.campuscart.common.domain.BaseEntity;
import com.campuscart.location.domain.City;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A college / university that students belong to.
 *
 * <p>Each college sits in exactly one {@link City} and owns one or more verified email
 * domains (see {@link CollegeEmailDomain}) used to gate student sign-up. A college name
 * is unique within its city.</p>
 */
@Entity
@Table(
        name = "colleges",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_colleges_name_city",
                columnNames = {"name", "city_id"}),
        indexes = @Index(name = "idx_colleges_city_id", columnList = "city_id"))
public class College extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "city_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_colleges_city"))
    private City city;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected College() {
        // Required by JPA.
    }

    public College(String name, City city) {
        this.name = name;
        this.city = city;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }

    public void update(String name, City city) {
        this.name = name;
        this.city = city;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isActive() {
        return active;
    }
}
