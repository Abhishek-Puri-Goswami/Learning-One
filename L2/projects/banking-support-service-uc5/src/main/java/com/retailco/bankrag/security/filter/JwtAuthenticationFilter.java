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
 * Deliverable: "JWT validation workflow" (the Spring Security wiring half;
 * secure-banking-core/.../JwtService.java is the cryptographic half,
 * already actually run). L2 HLD UseCase3 Implementation Approach step 1:
 * "Authenticate request using token validation."
 *
 * Runs once per request, before Spring Security's authorization checks:
 * extracts the Bearer token, delegates to the SAME JwtService class that
 * was compiled and run for real in secure-banking-core (not a
 * reimplementation), and populates the SecurityContext on success. On
 * failure, it does NOT reject the request itself -- it leaves the
 * SecurityContext empty and lets Spring Security's authorization rules
 * (SecurityConfig) return 401/403 uniformly, so an invalid-token response
 * looks identical to a missing-token response (no information leak about
 * which failure mode occurred, mirroring UnauthorizedException's Javadoc
 * reasoning in secure-banking-core).
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
