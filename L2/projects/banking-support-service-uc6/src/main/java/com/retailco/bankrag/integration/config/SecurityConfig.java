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
 * Same JWT wiring pattern as L2/UC3's SecurityConfig, narrowed to this
 * system's single unified endpoint, plus L2 HLD UseCase6's RBAC addition.
 *
 * Two RBAC enforcement layers, deliberately both present rather than
 * either/or:
 *   1. Service-layer RBAC (unchanged from UC3/UC5): {@code /api/v1/support/**}
 *      stays {@code permitAll()} at this gate because one endpoint serves
 *      both POLICY_QUESTION (no token needed) and LIVE_DATA (token-gated)
 *      intents -- {@code AccessPolicy}/{@code BankingToolService} make the
 *      real per-request decision, same as always.
 *   2. Method-level RBAC (new in UC6, {@code @EnableMethodSecurity}): admin
 *      endpoints that ARE single-purpose (e.g. AuditController's log-tail
 *      endpoint) use Spring Security's own {@code @PreAuthorize("hasRole(...)")}
 *      directly, because for those there's no per-intent nuance to
 *      delegate -- it's a flat "ADMIN or nothing" gate, which is exactly
 *      what annotation-based RBAC is for.
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
