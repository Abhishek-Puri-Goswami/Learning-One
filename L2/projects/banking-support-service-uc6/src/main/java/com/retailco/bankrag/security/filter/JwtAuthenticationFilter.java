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

// CONCEPT: Spring Security integration -- a custom servlet Filter that
// plugs JWT authentication into Spring's security filter chain.
// PURPOSE: Runs once per incoming HTTP request (extends
// OncePerRequestFilter -- a Spring Security base class that guarantees
// exactly one execution per request, even across internal forwards),
// BEFORE Spring Security's own authorization checks. Its job is only to
// figure out WHO is making the request (authentication), not to decide
// whether they're ALLOWED to do what they're asking (authorization --
// that's SecurityConfig's job).
//
// FLOW (see doFilterInternal() below, step by step):
// 1. Read the "Authorization: Bearer <token>" header.
// 2. Delegate the actual cryptographic verification to JwtService.verify()
//    -- this filter does NOT reimplement JWT logic, it only wires the
//    already-correct JwtService into Spring's request pipeline.
// 3. On a valid token: build a Spring Security `Authentication` object
//    from the token's claims (subject + roles, each role prefixed
//    "ROLE_" -- Spring Security's convention for role-based checks) and
//    store it in the SecurityContext, which is what `@PreAuthorize`/
//    `.hasRole(...)` rules elsewhere check against. The raw claims are
//    also stashed as a request attribute so controllers can do the
//    customer-id-match authorization check.
// 4. On an invalid/missing token: do nothing special here -- leave the
//    SecurityContext empty and call filterChain.doFilter() anyway.
//    Spring Security's own downstream rules then reject the (still
//    unauthenticated) request with a uniform 401 -- so a missing token
//    and an invalid token produce the SAME response, avoiding an
//    information leak about which one occurred (same reasoning as
//    UnauthorizedException).
//
// SPRING BOOT CONCEPT TO LEARN: this is the standard way to add custom
// authentication to Spring Security -- a Filter that populates
// SecurityContextHolder, registered into the filter chain by SecurityConfig
// (see that class for where this filter is actually added to the chain).
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
