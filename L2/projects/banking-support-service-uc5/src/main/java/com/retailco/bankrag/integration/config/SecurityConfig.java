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
 * Same JWT wiring pattern as L2/UC3's SecurityConfig, narrowed to this
 * system's single unified endpoint. Authentication here is OPTIONAL at
 * the Spring Security layer -- /api/v1/support/ask is reachable without a
 * token, since a POLICY_QUESTION intent never needs one (see
 * IntegratedBankingAssistant.handle's Javadoc); the per-request
 * authorization decision for LIVE_DATA intents happens inside
 * BankingToolService, same as it did in standalone UC3.
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
