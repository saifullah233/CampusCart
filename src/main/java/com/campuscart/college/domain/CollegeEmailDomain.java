package com.campuscart.college.domain;

import com.campuscart.common.domain.BaseEntity;
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
 * An email domain that maps a sign-up address to a {@link College}.
 *
 * <p>For example {@code iitb.ac.in} maps to IIT Bombay. A domain resolves to exactly
 * one college, so {@code domain} is globally unique; a college may register many
 * domains (e.g. departmental sub-domains). Domains are stored normalised to lower case
 * by the application layer.</p>
 */
@Entity
@Table(
        name = "college_email_domains",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_college_email_domains_domain",
                columnNames = "domain"),
        indexes = @Index(
                name = "idx_college_email_domains_college_id",
                columnList = "college_id"))
public class CollegeEmailDomain extends BaseEntity {

    @Column(name = "domain", nullable = false, length = 255)
    private String domain;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "college_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_college_email_domains_college"))
    private College college;

    protected CollegeEmailDomain() {
        // Required by JPA.
    }

    public CollegeEmailDomain(String domain, College college) {
        this.domain = domain;
        this.college = college;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public College getCollege() {
        return college;
    }

    public void setCollege(College college) {
        this.college = college;
    }
}
