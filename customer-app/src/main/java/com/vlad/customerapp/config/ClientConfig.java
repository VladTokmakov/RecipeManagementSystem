package com.vlad.customerapp.config;

import com.vlad.customerapp.client.RestClientProductsClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Slf4j
@Configuration
public class ClientConfig {

    @Bean
    public RestClientProductsClient restClientProductsClient(
            @Value("${vlad.services.catalogue.uri}") String catalogueBaseUrl,
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {

        OAuth2AuthorizedClientManager authorizedClientManager =
                new DefaultOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientRepository);

        return new RestClientProductsClient(RestClient.builder()
                .requestFactory(ClientHttpRequestFactoryBuilder.jdk()
                        .build(ClientHttpRequestFactorySettings.defaults()
                                .withConnectTimeout(Duration.ofSeconds(3))
                                .withReadTimeout(Duration.ofSeconds(5))))
                .baseUrl(catalogueBaseUrl)
                .requestInterceptor(new com.vlad.customerapp.security.OAuthClientHttpRequestInterceptor(
                        authorizedClientManager, "keycloak"))
                .build());
    }
}
