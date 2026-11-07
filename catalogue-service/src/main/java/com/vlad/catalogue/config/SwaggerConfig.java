package com.vlad.catalogue.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI catalogueServiceOpenAPI(
            @Value("${vlad.swagger.auth-url:https://localhost/auth/realms/RecipeManagementSystem/protocol/openid-connect/auth}")
            String authUrl,
            @Value("${vlad.swagger.token-url:https://localhost/auth/realms/RecipeManagementSystem/protocol/openid-connect/token}")
            String tokenUrl) {

        Scopes scopes = new Scopes()
                .addString("openid", "OpenID Connect")
                .addString("view_catalogue", "Чтение каталога")
                .addString("edit_catalogue", "Изменение каталога");

        OAuthFlow authorizationCode = new OAuthFlow()
                .authorizationUrl(authUrl)
                .tokenUrl(tokenUrl)
                .scopes(scopes);

        SecurityScheme oidc = new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .flows(new OAuthFlows().authorizationCode(authorizationCode));

        SecurityScheme bearer = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .info(new Info()
                        .title("Catalogue Service API")
                        .description("API для управления рецептами")
                        .version("1.0")
                        .contact(new Contact()
                                .name("Recipe Management System")
                                .email("support@recipes.com")))
                .components(new Components()
                        .addSecuritySchemes("keycloak", oidc)
                        .addSecuritySchemes("bearer-jwt", bearer))
                .addSecurityItem(new SecurityRequirement().addList("keycloak"));
    }
}
