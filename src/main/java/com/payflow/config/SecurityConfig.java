package com.payflow.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain security(HttpSecurity http, @Value("${app.api-key}") String apiKey) throws Exception {
        OncePerRequestFilter keyFilter = new OncePerRequestFilter() {
            @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
                String path = request.getServletPath();
                if (path.equals("/actuator/health") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) { chain.doFilter(request, response); return; }
                String provided = request.getHeader("X-API-Key");
                if (apiKey == null || apiKey.isBlank() || provided == null || !MessageDigest.isEqual(apiKey.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8))) {
                    response.setStatus(401); response.setContentType("application/json"); response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"A valid X-API-Key header is required\"}"); return;
                }
                chain.doFilter(request, response);
            }
        };
        return http.csrf(csrf -> csrf.disable()).sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(keyFilter, UsernamePasswordAuthenticationFilter.class).build();
    }
}
