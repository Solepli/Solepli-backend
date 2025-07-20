package com.ilta.solepli.global.config;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {

  @Value("${server.url}")
  private String serverUrl;

  @Value("${SWAGGER_URL}")
  private String swaggerUrl;

  @Bean
  public OpenAPI openAPI() {
    String jwt = "JWT";
    SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwt);
    Components components =
        new Components()
            .addSecuritySchemes(
                jwt,
                new SecurityScheme()
                    .name(jwt)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT"));

    ArrayList<Server> servers = new ArrayList<>();
    if (swaggerUrl.contains("localhost")) {
      servers.add(new Server().url(swaggerUrl).description("Local Server"));
    } else {
      servers.add(new Server().url(swaggerUrl).description("Solepli Server"));
    }

    return new OpenAPI()
        .components(new Components())
        .info(apiInfo())
        .addSecurityItem(securityRequirement)
        .servers(servers)
        .components(components);
  }

  private Info apiInfo() {
    return new Info()
        .title("Solepli REST API") // API의 제목
        .description("made by ilta Backend Team") // API에 대한 설명
        .contact(
            new Contact()
                .name("Solepli BE Github")
                .url("https://github.com/Solepli/Solepli-backend")) // BE 레포지토리
        // 주소
        .version("1.0.0"); // API의 버전
  }
}
