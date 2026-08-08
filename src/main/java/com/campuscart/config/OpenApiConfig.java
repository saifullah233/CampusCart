package com.campuscart.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger metadata for the CampusCart API.
 *
 * <p>Security schemes (JWT bearer) are intentionally not declared here yet; they are
 * added alongside the authentication module in a later part.</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI campusCartOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CampusCart API")
                        .description("Everything on Campus. By Students. For Students.")
                        .version("v1")
                        .contact(new Contact().name("CampusCart Backend"))
                        .license(new License().name("Proprietary")));
    }
}
