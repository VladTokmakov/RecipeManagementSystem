package com.vlad.catalogue.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;

@Configuration
public class SecurityBeans {

    private static final WebExpressionAuthorizationManager EDIT_CATALOGUE =
            new WebExpressionAuthorizationManager("hasAuthority('SCOPE_edit_catalogue') and hasRole('MANAGER')");

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {
        return http
                .authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/catalogue-api/products",
                                "/catalogue-api/products/{productId:\\d+}").permitAll()
                        .requestMatchers(HttpMethod.POST, "/catalogue-api/products").access(EDIT_CATALOGUE)
                        .requestMatchers(HttpMethod.PATCH, "/catalogue-api/products/{productId:\\d+}").access(EDIT_CATALOGUE)
                        .requestMatchers(HttpMethod.DELETE, "/catalogue-api/products/{productId:\\d+}").access(EDIT_CATALOGUE)
                        .anyRequest().denyAll())
                .csrf(CsrfConfigurer::disable)
                .sessionManagement(sessionManagement -> sessionManagement
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }
}
