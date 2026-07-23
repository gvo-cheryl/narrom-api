package com.naroom.api.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

	private static final String BEARER_AUTH_SCHEME_NAME = "bearerAuth";

	@Bean
	public OpenAPI naroomOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Naroom API")
						.description("Naroom 모바일 앱을 위한 백엔드 API")
						.version("v1"))
				.servers(List.of(new Server().url("/")))
				.components(new Components()
						.addSecuritySchemes(BEARER_AUTH_SCHEME_NAME, new SecurityScheme()
								.name(BEARER_AUTH_SCHEME_NAME)
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")));
	}

}
