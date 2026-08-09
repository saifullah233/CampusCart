package com.campuscart.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger metadata for the CampusCart API.
 *
 * <p>The bearer scheme documents the access-token boundary. Refresh tokens are opaque
 * credentials and are intentionally not represented as a JWT scheme.</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI campusCartOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .info(new Info()
                        .title("CampusCart API")
                        .description("Everything on Campus. By Students. For Students.")
                        .version("v1")
                        .contact(new Contact().name("CampusCart Backend"))
                        .license(new License().name("Proprietary")));
    }
}
