package com.campuscart.user.domain;

/**
 * Lifecycle state of a {@link User} account, controlling whether it may authenticate.
 *
 * <p>Only {@link #ACTIVE} accounts are permitted to obtain or use tokens. A freshly
 * created account starts {@link #PENDING_VERIFICATION} until email ownership is proven
 * by the OTP flow (added in a later part); {@link #SUSPENDED} is an operator action.
 * Status is managed server-side only and is never client-settable.</p>
 */
public enum AccountStatus {

    PENDING_VERIFICATION,
    ACTIVE,
    SUSPENDED;

    /** Whether an account in this state is allowed to authenticate. */
    public boolean canAuthenticate() {
        return this == ACTIVE;
    }
}
