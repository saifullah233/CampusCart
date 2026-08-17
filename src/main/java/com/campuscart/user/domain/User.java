package com.campuscart.user.domain;

import com.campuscart.college.domain.College;
import com.campuscart.common.domain.BaseEntity;
import com.campuscart.location.domain.City;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A registered CampusCart user.
 *
 * <p>Part 2 modelled the core identity (unique email, display name, {@link College}).
 * Part 3 adds the authentication dimension via the V4 migration: an optional password
 * hash, verification flags, account type, and the server-managed {@link Role} and
 * {@link AccountStatus}.</p>
 *
 * <p><strong>Security invariants.</strong> {@code role} and {@code status} are assigned
 * and mutated only by server-side logic — there is deliberately no setter for
 * {@code role}, and new instances always start as a {@link Role#STUDENT} with status
 * {@link AccountStatus#PENDING_VERIFICATION}. This makes privilege escalation via
 * request binding structurally impossible. The {@code passwordHash} is nullable because
 * it is populated by the OTP registration flow delivered in a later part; it stores an
 * adaptive (BCrypt) hash, never a plaintext or reversible value.</p>
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_users_email",
                columnNames = "email"),
        indexes = {
                @Index(name = "idx_users_college_id", columnList = "college_id"),
                @Index(name = "idx_users_city_id", columnList = "city_id")
        })
public class User extends BaseEntity {

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone_number", length = 32, unique = true)
    private String phoneNumber;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    /**
     * Adaptive (BCrypt) password hash. Nullable until the OTP registration flow sets it.
     */
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role = Role.STUDENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AccountStatus status = AccountStatus.PENDING_VERIFICATION;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private UserType accountType = UserType.STUDENT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "city_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_users_city"))
    private City city;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "college_id",
            nullable = true,
            foreignKey = @ForeignKey(name = "fk_users_college"))
    private College college;

    protected User() {
        // Required by JPA.
    }

    public User(String email, String fullName, College college) {
        this.email = email;
        this.fullName = fullName;
        this.city = college.getCity();
        this.college = college;
    }

    public User(String email, String fullName, College college, String phoneNumber) {
        this.email = email;
        this.fullName = fullName;
        this.city = college.getCity();
        this.college = college;
        this.phoneNumber = phoneNumber;
    }

    private User(String email, String fullName, City city, String phoneNumber) {
        this.email = email;
        this.fullName = fullName;
        this.city = city;
        this.phoneNumber = phoneNumber;
        this.accountType = UserType.COMMUNITY;
    }

    public static User student(String email, String fullName, College college, String phoneNumber) {
        return new User(email, fullName, college, phoneNumber);
    }

    public static User community(String email, String fullName, City city, String phoneNumber) {
        return new User(email, fullName, city, phoneNumber);
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    /** Stores an already-encoded password; raw credentials never enter the entity. */
    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public boolean isPhoneVerified() {
        return phoneVerified;
    }

    /** Marks proof of email ownership and activates the account. */
    public void markEmailVerified() {
        this.emailVerified = true;
        checkAndActivate();
    }

    /** Marks proof of phone ownership (retained for backward compatibility). */
    public void markPhoneVerified() {
        this.phoneVerified = true;
        checkAndActivate();
    }

    private void checkAndActivate() {
        if (emailVerified) {
            this.status = AccountStatus.ACTIVE;
        }
    }

    public Role getRole() {
        return role;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public UserType getAccountType() {
        return accountType;
    }

    /** Activates an account after email verification (test/fixture compatibility). */
    public void activateAfterEmailVerification() {
        this.emailVerified = true;
        this.status = AccountStatus.ACTIVE;
    }

    /** Activates an account after phone verification (test/fixture compatibility). */
    public void activateAfterPhoneVerification() {
        this.phoneVerified = true;
        this.status = AccountStatus.ACTIVE;
    }

    /** Retained as a compatibility alias for existing persistence fixtures. */
    public void activateAfterVerification() {
        this.emailVerified = true;
        this.phoneVerified = true;
        this.status = AccountStatus.ACTIVE;
    }

    /** Suspends the account through an operator-controlled server action. */
    public void suspend() {
        this.status = AccountStatus.SUSPENDED;
    }

    /** Restores a previously suspended, already-verified account. */
    public void reactivate() {
        this.status = AccountStatus.ACTIVE;
    }

    public College getCollege() {
        return college;
    }

    public City getCity() {
        return city;
    }

}
