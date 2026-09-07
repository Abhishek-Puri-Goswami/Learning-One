package com.retailco.bankrag.integration.config;

import com.retailco.bankrag.logging.HttpAccessLogFilter;
import com.retailco.bankrag.security.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * The same JWT-based security setup used earlier in this project,
 * plus one addition: role-based checks placed directly on individual
 * methods (turned on by {@code @EnableMethodSecurity}), for endpoints
 * where the rule is simple and fixed.
 * <p>
 * There are two layers of access control here, on purpose, not as a
 * redundancy:
 * <ol>
 *   <li>Path-based rules, like before: {@code /api/v1/support/**} stays
 *       open at this gate, because one endpoint serves both policy
 *       questions (which need no token) and live-data questions (which
 *       do) — {@code AccessPolicy} and {@code BankingToolService} make
 *       the real per-request decision, same as always.</li>
 *   <li>Method-level rules, new here: an admin-only endpoint (like
 *       {@code AuditController}'s log-viewing endpoint) uses Spring
 *       Security's own {@code @PreAuthorize("hasRole(...)")} directly,
 *       because there's no per-question nuance to it — it's a flat
 *       "admin, or nothing" gate, which is exactly what this kind of
 *       annotation is for.</li>
 * </ol>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
                                                      HttpAccessLogFilter httpAccessLogFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/dev/**").permitAll()
                        .requestMatchers("/api/v1/support/**").permitAll() // per-intent authorization enforced inside the service layer, not at this gate
                        .requestMatchers("/api/v1/admin/**").authenticated() // must carry a valid JWT; role check happens via @PreAuthorize below
                        .anyRequest().denyAll()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(httpAccessLogFilter, JwtAuthenticationFilter.class); // structured HTTP access log, see HttpAccessLogFilter's Javadoc
        return http.build();
    }
}
