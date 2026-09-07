package com.retailco.bankrag.integration.config;

import com.retailco.bankrag.security.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Declares Spring Security's HTTP filter chain: which paths need a
 * login token, the session policy, and where our custom
 * {@code JwtAuthenticationFilter} plugs into Spring's built-in pipeline.
 * This sets the application up as a stateless, token-based API — no
 * server-side sessions and no CSRF tokens needed, since there's no
 * browser-form-based login here at all.
 * <p>
 * Walking through {@code securityFilterChain()} below, one call at a
 * time:
 * <ul>
 *   <li>{@code csrf().disable()}: CSRF protection defends against
 *       attacks on cookie/session-based browser logins; a stateless
 *       token-based API that sends its token in a header isn't
 *       vulnerable to that particular attack, so it's safe to turn
 *       off here.</li>
 *   <li>{@code sessionManagement(... STATELESS)}: tells Spring Security
 *       to never create or use a session — every request has to carry
 *       its own proof of identity (the token), since
 *       {@code JwtAuthenticationFilter} checks it fresh on every single
 *       request.</li>
 *   <li>{@code authorizeHttpRequests(...)}: the actual access rules,
 *       checked top to bottom, first match wins. Notice
 *       {@code /api/v1/support/**} is allowed through at this layer —
 *       the real per-question authorization for live banking data still
 *       happens one layer down, inside {@code BankingToolService}. This
 *       is deliberate: a policy question never needs a token at all, so
 *       this gate can't reject by default the way it safely could for
 *       an endpoint that's always admin-only.</li>
 *   <li>{@code addFilterBefore(jwtFilter, ...)}: inserts
 *       {@code JwtAuthenticationFilter} into Spring's chain BEFORE the
 *       standard username/password filter, so a caller's identity from
 *       their token is established early, before any authorization
 *       checks run.</li>
 * </ul>
 */
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
