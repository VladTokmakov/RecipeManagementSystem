package com.vlad.catalogue.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Configuration
@ConditionalOnProperty(prefix = "spring.security.oauth2.client.registration.keycloak-swagger", name = "client-id")
public class SwaggerLoginConfig {

    static final String REGISTRATION_ID = "keycloak-swagger";
    static final String LOGIN_BASE_URI = "/catalogue-auth";

    @Bean
    @Order(1)
    public SecurityFilterChain swaggerSecurityFilterChain(
            HttpSecurity http,
            OAuth2UserService<OidcUserRequest, OidcUser> swaggerOidcUserService) throws Exception {

        return http
                .securityMatcher("/swagger-ui.html", "/swagger-ui/**",
                        "/v3/api-docs", "/v3/api-docs/**", LOGIN_BASE_URI + "/**")
                .authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests
                        .requestMatchers(LOGIN_BASE_URI + "/**").permitAll()
                        .anyRequest().hasRole("MANAGER"))
                .csrf(CsrfConfigurer::disable)
                .sessionManagement(sessionManagement -> sessionManagement
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .oauth2Login(oauth2Login -> oauth2Login
                        .authorizationEndpoint(authorizationEndpoint -> authorizationEndpoint
                                .baseUri(LOGIN_BASE_URI + "/oauth2/authorization"))
                        .redirectionEndpoint(redirectionEndpoint -> redirectionEndpoint
                                .baseUri(LOGIN_BASE_URI + "/login/oauth2/code/*"))
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(swaggerOidcUserService))
                        .defaultSuccessUrl("/swagger-ui/index.html", true))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint(
                                LOGIN_BASE_URI + "/oauth2/authorization/" + REGISTRATION_ID)))
                .build();
    }

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> swaggerOidcUserService() {
        OidcUserService delegate = new OidcUserService();

        return request -> {
            OidcUser user = delegate.loadUser(request);

            Set<GrantedAuthority> authorities = new LinkedHashSet<>(user.getAuthorities());
            if (user.getClaims().get("realm_access") instanceof Map<?, ?> realmAccess
                    && realmAccess.get("roles") instanceof Collection<?> roles) {
                roles.forEach(role -> authorities.add(new SimpleGrantedAuthority(role.toString())));
            }

            return new DefaultOidcUser(authorities, user.getIdToken(), user.getUserInfo());
        };
    }
}
