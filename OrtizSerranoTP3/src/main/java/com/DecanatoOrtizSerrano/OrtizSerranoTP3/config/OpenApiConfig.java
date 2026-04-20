package com.DecanatoOrtizSerrano.OrtizSerranoTP3.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Decanato API – OrtizSerrano TP3")
                .version("1.0.0")
                .description("""
                    API REST del sistema de gestión del Decanato universitario.
                    
                    **Roles disponibles:**
                    - `ADMINISTRADOR` → acceso a `/api/admin/**` (materias, períodos, estudiantes)
                    - `DOCENTE` → acceso a su propio dashboard
                    - `ESTUDIANTE` → acceso a su propio dashboard
                    
                    **Autenticación:** JWT Bearer Token.
                    Hacer login en `POST /api/auth/login` y copiar el token en el botón **Authorize 🔒**.
                    """)
                .contact(new Contact()
                    .name("Ortiz & Serrano")
                    .email("ortiz.serrano@ipm.edu.ar"))
            )
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Servidor de desarrollo")
            ))
            .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
            .components(new Components()
                .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                    .name(BEARER_AUTH)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Ingresá el token JWT obtenido en /api/auth/login")
                )
            );
    }
}
