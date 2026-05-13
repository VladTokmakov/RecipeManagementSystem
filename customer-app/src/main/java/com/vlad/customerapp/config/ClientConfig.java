package com.vlad.customerapp.config;

import com.vlad.customerapp.client.RestClientProductsClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientConfig {

    @Bean
    public RestClientProductsClient restClientProductsClient(
            @Value("${vlad.services.catalogue.uri:http://localhost:8081}") String catalogueBaseUrl
    ) {
        return new RestClientProductsClient(RestClient.builder()
                .baseUrl(catalogueBaseUrl)
                .build());
    }
}
