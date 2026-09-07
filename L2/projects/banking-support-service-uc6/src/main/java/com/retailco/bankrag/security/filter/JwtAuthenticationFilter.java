package com.retailco.bankrag.security.filter;

import com.retailco.bankrag.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * This class plugs JWT authentication into Spring Security's request
 * pipeline. It runs once for every incoming HTTP request, BEFORE Spring
 * Security's own authorization checks. Its job is ONLY to figure out WHO
 * is making the request (authentication) — not to decide whether they're
 * allowed to do what they're asking (authorization is
 * {@code SecurityConfig}'s job).
 * <p>
 * Here's what {@code doFilterInternal()} does, step by step:
 * <ol>
 *   <li>Read the {@code Authorization: Bearer <token>} header.</li>
 *   <li>Hand the actual cryptographic checking off to
 *       {@code JwtService.verify()} — this filter doesn't reimplement any
 *       JWT logic itself, it just wires the already-correct
 *       {@code JwtService} into Spring's pipeline.</li>
 *   <li>If the token is valid: build a Spring Security "Authentication"
 *       object from its claims and store it where Spring Security's own
 *       rules can check it later.</li>
 *   <li>If the token is invalid or missing: do nothing special — leave
 *       the request unauthenticated and let it continue. Spring
 *       Security's own downstream rules then reject it with a uniform
 *       401 error — so a missing token and an invalid token look
 *       identical to the caller, which avoids leaking information about
 *       which one happened.</li>
 * </ol>
 * (See {@code SecurityConfig} for where this filter is actually added to
 * the chain.)
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring("Bearer ".length());
            JwtService.VerificationResult result = jwtService.verify(token);

            if (result instanceof JwtService.Valid valid) {
                JwtService.Claims claims = valid.claims();
                List<GrantedAuthority> authorities = claims.roles().stream()
                        .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();
                var authentication = new UsernamePasswordAuthenticationToken(claims.subject(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                // Also expose the raw claims as a request attribute so
                // controllers can perform the customer-id-match
                // authorization check (see BankingToolService's
                // verifyAndAuthorize -- reused here, not duplicated).
                request.setAttribute("jwtClaims", claims);
            }
            // Invalid token: SecurityContext stays empty; downstream
            // authorization rules handle the 401.
        }

        filterChain.doFilter(request, response);
    }
}
