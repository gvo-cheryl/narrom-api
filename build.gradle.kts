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

	runtimeOnly("org.flywaydb:flyway-database-postgresql")
	runtimeOnly("org.postgresql:postgresql")

	/* Test */
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
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

tasks.register("generateOpenApiDocs") {
	group = "documentation"
	description = "Boots the application and fetches /v3/api-docs.yaml into docs/api/openapi.yaml"
	dependsOn("classes")

	doLast {
		val javaExecutable = javaToolchains.launcherFor(java.toolchain).get().executablePath.asFile
		val classpath = sourceSets["main"].runtimeClasspath.asPath
		val logFile = layout.buildDirectory.file("openapi-docs/bootRun.log").get().asFile
		logFile.parentFile.mkdirs()

		val process = ProcessBuilder(
			javaExecutable.absolutePath,
			"-cp", classpath,
			"-Dserver.port=$openApiDocsPort",
			"-Dspring.profiles.active=local",
			"com.naroom.api.NaroomApiApplication"
		)
			.directory(rootDir)
			.redirectErrorStream(true)
			.redirectOutput(logFile)
			.start()

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
