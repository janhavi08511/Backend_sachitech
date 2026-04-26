package com.example.sachitech.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // ✅ ADD THIS
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.example.sachitech.security.JwtAuthFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity // ✅ Required for @PreAuthorize
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public org.springframework.security.authentication.AuthenticationManager authenticationManager(org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth  
                // Public Endpoints
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/auth/login").permitAll()
                .requestMatchers("/uploads/**").permitAll() 
                .requestMatchers("/internship-placement/placements").permitAll()
                .requestMatchers("/api/admin/create-user").hasRole("ADMIN")                
                // Admin Only
                .requestMatchers("/auth/createuser/").hasRole("ADMIN")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // Reports (Management Only)
                .requestMatchers("/reports/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "MANAGER")
                
                // Restricted but viewable by Students
                .requestMatchers("/fees/**").hasAnyRole("ADMIN", "MANAGER", "STUDENT")
                .requestMatchers("/api/fees/**").hasAnyRole("ADMIN", "MANAGER", "STUDENT")
                .requestMatchers("/performance/**").hasAnyRole("ADMIN", "MANAGER", "TRAINER", "STUDENT")
                .requestMatchers("/attendance/**").hasAnyRole("ADMIN", "MANAGER", "TRAINER", "STUDENT")
                .requestMatchers("/api/performance/**").hasAnyRole("ADMIN", "MANAGER", "TRAINER", "STUDENT")
                .requestMatchers("/api/attendance/**").hasAnyRole("ADMIN", "MANAGER", "TRAINER", "STUDENT")
                .requestMatchers("/api/trainer/**").hasAnyRole("ADMIN", "TRAINER")
                
                // LMS endpoints – role-level access enforced via @PreAuthorize in LmsController
                .requestMatchers("/api/lms/**").authenticated()

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
