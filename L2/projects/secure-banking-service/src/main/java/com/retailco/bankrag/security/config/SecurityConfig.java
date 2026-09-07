package com.retailco.bankrag.security.config;

import com.retailco.bankrag.security.BankingDataStore;
import com.retailco.bankrag.security.BankingToolService;
import com.retailco.bankrag.security.IntentClassifier;
import com.retailco.bankrag.security.JwtService;
import com.retailco.bankrag.security.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Wires JwtAuthenticationFilter into the Spring Security filter chain,
 * disables session creation (stateless JWT auth, per L2 HLD UseCase3's
 * "JWT-based authentication" -- no server-side session store needed),
 * and requires authentication for every /api/v1/banking/** endpoint.
 *
 * The JwtService bean's signing secret is externalized via
 * bankrag.security.jwt-secret (see application.yml) rather than
 * hardcoded, per security/security-validation-checklist.md's secret-
 * management item -- consistent with L1/UC4's fix for the same class of
 * issue (hardcoded payment API key -> @Value-injected).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${bankrag.security.jwt-secret}")
    private String jwtSecret;

    @Value("${bankrag.security.jwt-expiry-seconds:900}")
    private long jwtExpirySeconds;

    @Bean
    public JwtService jwtService() {
        return new JwtService(jwtSecret, jwtExpirySeconds);
    }

    @Bean
    public BankingDataStore bankingDataStore() {
        return new BankingDataStore();
    }

    @Bean
    public BankingToolService bankingToolService(JwtService jwtService, BankingDataStore bankingDataStore) {
        return new BankingToolService(jwtService, bankingDataStore);
    }

    @Bean
    public IntentClassifier intentClassifier() {
        return new IntentClassifier();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // stateless bearer-token API, no cookie-based session to protect
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/dev/**").permitAll() // DEV_TOKEN_ONLY -- see DevTokenController's Javadoc and the security checklist item on this
                        .requestMatchers("/api/v1/banking/**").authenticated()
                        .anyRequest().denyAll()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
