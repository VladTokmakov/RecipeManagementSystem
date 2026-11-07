package com.vlad.managerapp.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "vlad.services.catalogue")
public record CatalogueProperties(@NotBlank String uri, @NotBlank String registrationId) {
}
