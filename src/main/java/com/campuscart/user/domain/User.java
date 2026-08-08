package com.campuscart.user.domain;

import com.campuscart.college.domain.College;
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
 * A registered CampusCart user (a student).
 *
 * <p>Part 2 intentionally models only the core identity: a unique email, a display
 * name, and the {@link College} the user belongs to. Authentication concerns
 * (password hash, email-verification flag, role, account status) are added by the
 * auth module in a later part via a follow-up migration, so they are deliberately
 * absent here rather than stubbed.</p>
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_users_email",
                columnNames = "email"),
        indexes = @Index(name = "idx_users_college_id", columnList = "college_id"))
public class User extends BaseEntity {

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "college_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_users_college"))
    private College college;

    protected User() {
        // Required by JPA.
    }

    public User(String email, String fullName, College college) {
        this.email = email;
        this.fullName = fullName;
        this.college = college;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public College getCollege() {
        return college;
    }

    public void setCollege(College college) {
        this.college = college;
    }
}
