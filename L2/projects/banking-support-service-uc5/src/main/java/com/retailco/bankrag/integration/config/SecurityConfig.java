package com.retailco.bankrag.integration.config;

import com.retailco.bankrag.security.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// CONCEPT: Spring Security configuration -- declaring the HTTP security
// filter chain: which paths need authentication, session policy, and
// where custom filters (like JwtAuthenticationFilter) plug into Spring's
// built-in filter chain.
// PURPOSE: Configures this application as a stateless, JWT-based API (no
// server-side sessions, no CSRF tokens needed since there are no
// browser-form-based logins here).
//
// HOW THE FILTER CHAIN IS BUILT (see securityFilterChain() below, each
// call configures one aspect):
// - `.csrf(csrf -> csrf.disable())`: CSRF protection defends
//   cookie/session-based browser logins; a stateless JWT API sending
//   tokens in headers isn't vulnerable to that attack, so it's safely
//   disabled here.
// - `.sessionManagement(... STATELESS)`: tells Spring Security to never
//   create or use an HttpSession -- every request must carry its own
//   proof of identity (the JWT), matching how JwtAuthenticationFilter
//   re-authenticates on every single request.
// - `.authorizeHttpRequests(...)`: the actual access rules, checked
//   top-to-bottom, first match wins. Notice `/api/v1/support/**` is
//   `permitAll()` at THIS layer -- Spring Security lets the request
//   through, but per-request authorization for LIVE_DATA intents still
//   happens one layer down, inside BankingToolService. This is a
//   deliberate design: a POLICY_QUESTION never needs a token at all, so
//   this gate can't reject-by-default the way it could for an
//   admin-only endpoint (compare AuditController's `@PreAuthorize`, a
//   much simpler all-or-nothing rule).
// - `.addFilterBefore(jwtFilter, ...)`: inserts JwtAuthenticationFilter
//   into Spring's filter chain BEFORE the standard username/password
//   filter, so JWT-based identity is established early, before any
//   authorization checks run.
//
// SPRING BOOT CONCEPT TO LEARN: `@EnableWebSecurity` + a `SecurityFilterChain`
// @Bean is the modern (Spring Security 6+) way to configure security --
// replaces the older `WebSecurityConfigurerAdapter` subclassing style.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/dev/**").permitAll()
                        .requestMatchers("/api/v1/support/**").permitAll() // per-intent authorization enforced inside the service layer, not at this gate
                        .anyRequest().denyAll()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
