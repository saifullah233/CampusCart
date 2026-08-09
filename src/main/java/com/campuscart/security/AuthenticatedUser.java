package com.campuscart.security;

import com.campuscart.user.domain.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The authenticated principal for a request, reconstructed solely from a verified
 * access token — never from client-supplied identity fields.
 *
 * <p>Placed in the {@code SecurityContext} and injectable via
 * {@code @AuthenticationPrincipal}. Ownership and authorization decisions must read
 * {@link #id()} and {@link #role()} from this object (i.e. from the signed token),
 * which is why there is no client-facing path to set them.</p>
 *
 * <p>Implements {@link UserDetails} for framework interoperability. It is stateless:
 * {@link #getPassword()} is always {@code null} because authentication was already
 * proven by the token signature, and the account-status flags are {@code true} because
 * account state is validated at token-issuance time.</p>
 */
public final class AuthenticatedUser implements UserDetails {

    private final UUID id;
    private final String email;
    private final Role role;
    private final List<GrantedAuthority> authorities;

    public AuthenticatedUser(UUID id, String email, Role role) {
        this.id = Objects.requireNonNull(id, "id");
        this.email = Objects.requireNonNull(email, "email");
        this.role = Objects.requireNonNull(role, "role");
        this.authorities = List.of(new SimpleGrantedAuthority(role.authority()));
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public Role role() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /** Not applicable to a token-derived principal; always {@code null}. */
    @Override
    public String getPassword() {
        return null;
    }

    /** The stable login identifier (email). */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuthenticatedUser other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        // No PII beyond the id: email is deliberately omitted to keep it out of logs.
        return "AuthenticatedUser[id=" + id + ", role=" + role + "]";
    }
}
