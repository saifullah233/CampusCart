package com.campuscart.security;

import com.campuscart.common.exception.ErrorCode;
import com.campuscart.common.exception.InvalidTokenException;
import com.campuscart.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates a request from its {@code Authorization: Bearer <jwt>} header.
 *
 * <p>On a valid access token, an {@link AuthenticatedUser} is placed in the
 * {@link SecurityContext} for the duration of the request. A request with no token, or
 * with an invalid one, is left unauthenticated and simply proceeds down the chain — the
 * {@code AuthenticationEntryPoint} then decides whether the target endpoint requires
 * authentication. This keeps the 401 response uniform and avoids leaking why a token was
 * rejected.</p>
 *
 * <p>Invalid tokens are left unauthenticated for the entry point to handle. Persisted
 * inactive accounts are rejected here so an existing access token cannot reach any
 * protected endpoint after suspension.</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final SecurityErrorResponseWriter errorResponseWriter;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   UserRepository userRepository,
                                   SecurityErrorResponseWriter errorResponseWriter) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveBearerToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                AuthenticatedUser principal = jwtService.parseAccessToken(token);
                var persistedUser = userRepository.findById(principal.id());
                if (persistedUser.isPresent()) {
                    var user = persistedUser.get();
                    if (!user.getStatus().canAuthenticate()) {
                        SecurityContextHolder.clearContext();
                        errorResponseWriter.write(response, HttpServletResponse.SC_FORBIDDEN,
                                ErrorCode.ACCOUNT_NOT_ACTIVE.name(), "The account is not active.",
                                request.getRequestURI());
                        return;
                    }
                    principal = new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole());
                }
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            } catch (InvalidTokenException ex) {
                // Leave the context unauthenticated; the entry point handles protected
                // endpoints. Nothing is written here so the response envelope stays uniform.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String value = header.substring(BEARER_PREFIX.length()).trim();
            return value.isEmpty() ? null : value;
        }
        return null;
    }
}
