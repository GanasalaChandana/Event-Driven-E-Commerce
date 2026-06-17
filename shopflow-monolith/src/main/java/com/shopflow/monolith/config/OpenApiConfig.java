package com.shopflow.monolith.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

@Configuration
public class OpenApiConfig {

    static {
        SpringDocUtils.getConfig()
                .addAnnotationsToIgnore(AuthenticationPrincipal.class)
                .addRequestWrapperToIgnore(UserDetails.class, Authentication.class);
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ShopFlow API")
                        .description("Event-driven e-commerce REST API — auth, products, inventory, orders")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Chandana Ganasala")
                                .url("https://github.com/GanasalaChandana/Event-Driven-E-Commerce")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Auth"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Auth", new SecurityScheme()
                                .name("Bearer Auth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
