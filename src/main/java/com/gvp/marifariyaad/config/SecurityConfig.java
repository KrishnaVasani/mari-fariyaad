package com.gvp.marifariyaad.config;

import com.gvp.marifariyaad.security.CustomUserDetailsService;
import com.gvp.marifariyaad.security.RestAccessDeniedHandler;
import com.gvp.marifariyaad.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Real, session-based Spring Security configuration.
 *
 * Authentication itself happens in AuthController (POST /api/auth/login), which
 * validates credentials via the AuthenticationManager below and stores the resulting
 * SecurityContext in the HTTP session - this lets the frontend keep using simple JSON
 * fetch() calls instead of Spring Security's default HTML login form, while every
 * subsequent request is still protected by a genuine server-side session.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(restAuthenticationEntryPoint)
                    .accessDeniedHandler(restAccessDeniedHandler))
            .authorizeHttpRequests(auth -> auth
                    // Public static assets
                    .requestMatchers("/css/**", "/js/**", "/lang/**", "/uploads/**", "/favicon.ico").permitAll()
                    // Public pages
                    .requestMatchers(
                            "/", "/index.html", "/about.html", "/faq.html", "/contact.html",
                            "/login.html", "/admin-login.html", "/register.html",
                            "/forgot-password.html"
                    ).permitAll()
                    // Public auth API endpoints
                    .requestMatchers(
                            "/api/auth/register",
                            "/api/auth/verify-registration",
                            "/api/auth/verify-registration-otp",
                            "/api/auth/resend-registration-otp",
                            "/api/auth/login",
                            "/api/auth/forgot-password",
                            "/api/auth/verify-reset-otp",
                            "/api/auth/verify-forgot-password-otp",
                            "/api/auth/reset-password"
                    ).permitAll()
                    // Protected pages requiring login
                    .requestMatchers(
                            "/complaint.html", "/track.html", "/dashboard.html", "/profile.html",
                            "/departments.html", "/hostels.html"
                    ).authenticated()
                    // Admin-only page
                    .requestMatchers("/admin-dashboard.html").hasRole("ADMIN")
                    // Admin-only API
                    .requestMatchers("/api/complaints/admin/**").hasRole("ADMIN")
                    // Everything else under /api requires authentication
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().permitAll())
            .authenticationProvider(authenticationProvider());

        return http.build();
    }
}
