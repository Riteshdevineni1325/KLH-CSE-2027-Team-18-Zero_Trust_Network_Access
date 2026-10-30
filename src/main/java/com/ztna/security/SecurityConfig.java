package com.ztna.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final ZeroTrustSessionFilter zeroTrustSessionFilter;

    public SecurityConfig(ZeroTrustSessionFilter zeroTrustSessionFilter) {
        this.zeroTrustSessionFilter = zeroTrustSessionFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .addFilterBefore(zeroTrustSessionFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // Static frontend assets
                .requestMatchers(
                    "/", "/index.html", "/app.css", "/app.js",
                    "/favicon.ico", "/static/**", "/*.html", "/*.css", "/*.js"
                ).permitAll()
                // Public API: auth, access requests, health check
                .requestMatchers(
                    "/api/auth/**",
                    "/api/access/**",
                    "/api/health"
                ).permitAll()
                // Audit read paths (role enforcement handled by ZeroTrustSessionFilter)
                .requestMatchers("/api/audit/**").authenticated()
                // Admin paths (role + super key enforcement handled by ZeroTrustSessionFilter)
                .requestMatchers("/api/admin/**").authenticated()
                // Protected resources (continuous eval handled by ZeroTrustSessionFilter)
                .requestMatchers("/api/protected/**").authenticated()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
