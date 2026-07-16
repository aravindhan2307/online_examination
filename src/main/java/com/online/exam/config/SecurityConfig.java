package com.online.exam.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for simplified REST APIs and H2 Console
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin())) // Allow H2 Console frames
            .authorizeHttpRequests(auth -> auth
                // Public static resources
                .requestMatchers("/css/**", "/js/**", "/index.html", "/", "/favicon.ico").permitAll()
                // Candidate workflows
                .requestMatchers("/exam.html", "/result.html").permitAll()
                // Admin static pages (secured by Spring Security login redirect on API calls)
                .requestMatchers("/arrear.html").permitAll()
                .requestMatchers("/api/questions/exam", "/api/questions/start", "/api/results/submit").permitAll()
                // Re-exam fee payment by candidate (unauthenticated)
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/fees/pay/**").permitAll()
                // Arrear list requires admin authentication
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/fees/arrears").hasRole("ADMIN")
                // Custom login page
                .requestMatchers("/login.html").permitAll()
                // Developer database console
                .requestMatchers("/h2-console/**").permitAll()
                // Protected administrator dashboards
                .requestMatchers("/admin.html", "/history.html").hasRole("ADMIN")
                // Protected admin REST endpoints
                .requestMatchers("/api/questions/**", "/api/results/**").hasRole("ADMIN")
                // All other routes require authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/admin.html", true)
                .failureUrl("/login.html?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/index.html")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder.encode("admin123"))
            .roles("ADMIN")
            .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
