import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.TimeUnit

plugins {
	java
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.naroom"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	developmentOnly("org.springframework.boot:spring-boot-devtools")

	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-flyway")

	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")

	implementation("org.springframework.boot:spring-boot-starter-security")

	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

	runtimeOnly("org.flywaydb:flyway-database-postgresql")
	runtimeOnly("org.postgresql:postgresql")

	/* Test */
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
	testLogging {
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
		showCauses = true
		showStackTraces = true
	}
}

val openApiDocsPort = 8099
val openApiDocsProfile = System.getenv("OPENAPI_DOCS_PROFILE") ?: "local"

tasks.register("generateOpenApiDocs") {
	group = "documentation"
	description = "Boots the application and fetches /v3/api-docs.yaml into docs/api/openapi.yaml"
	dependsOn("classes")

	doLast {
		val javaExecutable = javaToolchains.launcherFor(java.toolchain).get().executablePath.asFile
		val classpath = sourceSets["main"].runtimeClasspath.asPath
		val logFile = layout.buildDirectory.file("openapi-docs/bootRun.log").get().asFile
		logFile.parentFile.mkdirs()

		val processBuilder = ProcessBuilder(
			javaExecutable.absolutePath,
			"-cp", classpath,
			"-Dserver.port=$openApiDocsPort",
			"-Dspring.profiles.active=$openApiDocsProfile",
			// spring-boot-devtools의 재시작 클래스로더가 이 단발성 boot에서 config-import(.env.local 등)
			// 처리와 경합해 간헐적으로 실패한다(재현 확인됨). 문서 생성에는 live-reload가 필요 없으므로 끈다.
			"-Dspring.devtools.restart.enabled=false",
			"com.naroom.api.NaroomApiApplication"
		)
			.directory(rootDir)
			.redirectErrorStream(true)
			.redirectOutput(logFile)

		// Gradle daemon 프로세스 환경에 "빈 문자열"인 secret 변수가 남아 있으면(로컬에서 재현 확인됨) OS
		// 환경변수가 .env.local의 config-import 값보다 우선순위가 높아 빈 값으로 덮어써 버린다. 값이 실제로
		// 채워져 있는 경우(CI는 JWT_SECRET 등을 워크플로에서 직접 주입한다)는 그대로 둔다 — 빈 값만 제거한다.
		listOf("JWT_SECRET", "DB_PASSWORD", "REDIS_PASSWORD", "OPENAI_API_KEY").forEach { key ->
			val inherited = processBuilder.environment()[key]
			if (inherited != null && inherited.isBlank()) {
				processBuilder.environment().remove(key)
			}
		}

		val process = processBuilder.start()

		try {
			val deadline = System.currentTimeMillis() + 120_000
			var started = false
			while (System.currentTimeMillis() < deadline) {
				if (logFile.readText().contains("Started NaroomApiApplication")) {
					started = true
					break
				}
				if (!process.isAlive) break
				Thread.sleep(1000)
			}
			if (!started) {
				throw GradleException("Application did not report a successful start within timeout. See $logFile")
			}

			val connection = URI("http://localhost:$openApiDocsPort/v3/api-docs.yaml")
				.toURL()
				.openConnection() as HttpURLConnection
			connection.connectTimeout = 5000
			connection.readTimeout = 15000

			val status = connection.responseCode
			if (status != 200) {
				throw GradleException("OpenAPI endpoint returned HTTP $status")
			}

			val body = connection.inputStream.bufferedReader().use { it.readText() }
			if (body.isBlank()) {
				throw GradleException("Generated OpenAPI document is empty")
			}
			if (!body.trimStart().startsWith("openapi: \"3.1") && !body.trimStart().startsWith("openapi: 3.1")) {
				throw GradleException("Generated document is not OpenAPI 3.1: ${body.lineSequence().first()}")
			}

			val outputFile = file("docs/api/openapi.yaml")
			outputFile.parentFile.mkdirs()
			outputFile.writeText(body)

			logger.lifecycle("Generated ${outputFile.relativeTo(rootDir)}")
		} finally {
			process.destroy()
			if (!process.waitFor(10, TimeUnit.SECONDS)) {
				process.destroyForcibly()
			}
		}
	}
}
