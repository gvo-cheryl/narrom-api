package com.naroom.api.global.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTest {

	private static final String[] FORBIDDEN_SUBSTRINGS = {
			"password", "secret", "supabase.com", "aws-0-ap-northeast"
	};

	@Autowired
	private MockMvc mockMvc;

	@Test
	void openApiYaml_hasRequiredStructure() throws Exception {
		String body = mockMvc.perform(get("/v3/api-docs.yaml"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString(StandardCharsets.UTF_8);

		assertFalse(body.isBlank(), "generated OpenAPI document must not be empty");

		String lowerCaseBody = body.toLowerCase();
		for (String forbidden : FORBIDDEN_SUBSTRINGS) {
			assertFalse(lowerCaseBody.contains(forbidden), "document must not contain: " + forbidden);
		}

		JsonNode root = new YAMLMapper().readTree(body);

		assertTrue(root.path("openapi").asText().startsWith("3.1"), "openapi version must start with 3.1");
		assertEquals("Naroom API", root.path("info").path("title").asText());
		assertEquals("v1", root.path("info").path("version").asText());

		JsonNode bearerAuth = root.path("components").path("securitySchemes").path("bearerAuth");
		assertFalse(bearerAuth.isMissingNode(), "bearerAuth security scheme must be defined");
		assertEquals("http", bearerAuth.path("type").asText());
		assertEquals("bearer", bearerAuth.path("scheme").asText());

		assertTrue(root.path("paths").has("/api/v1/health"), "health API must be documented");
	}

}
