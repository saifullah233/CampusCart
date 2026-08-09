package com.campuscart.user.domain;

/**
 * Coarse-grained authorization role assigned to a {@link User}.
 *
 * <p>Roles are assigned server-side only — never accepted from client input — so a
 * client can never escalate its own privileges. A new account defaults to
 * {@link #STUDENT}; {@link #ADMIN} is granted out-of-band by an operator.</p>
 *
 * <p>The {@link #authority()} string is the value Spring Security matches on; the
 * {@code ROLE_} prefix is the framework convention that {@code hasRole('ADMIN')} and
 * {@code @PreAuthorize} expand to.</p>
 */
public enum Role {

    STUDENT,
    ADMIN;

    /** Spring Security authority string, e.g. {@code ROLE_ADMIN}. */
    public String authority() {
        return "ROLE_" + name();
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }
}
