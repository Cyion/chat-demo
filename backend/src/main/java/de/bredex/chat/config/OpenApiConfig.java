package de.bredex.chat.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Chat API")
                        .version("1.0")
                        .description("""
                                REST API for a 1:1 chat application.
                                
                                **Scope:** Chat/participant management and user search only. \
                                Messages are not implemented but the data model supports adding them later.
                                
                                **Auth:** JWT Bearer token (stateless, 24h expiry, no refresh tokens). \
                                Refresh tokens are omitted to reduce complexity — the 24h window is sufficient for this use case.
                                
                                **User search:** Includes the authenticated user in results (filtering is a UI concern). \
                                Rate limiting is recommended for production but not implemented here.
                                
                                **Chat dedup:** A chat between the same two users cannot be created twice \
                                (enforced via a unique participant hash).
                                """))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
