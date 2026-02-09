plugins {
	id("org.springframework.boot") version "3.5.10" // 4.0.2 대신 이걸 쓰세요. 훠얼씬 안정적입니다.
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("jvm") version "1.9.24"
	kotlin("plugin.spring") version "1.9.24"
	kotlin("plugin.jpa") version "1.9.24"
	kotlin("kapt") version "1.9.24"
}

group = "com.hirelog"
version = "0.0.1-SNAPSHOT"
description = "HireLog backend API"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
	google()
}

dependencies {
	implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.kafka:spring-kafka")
	implementation("com.querydsl:querydsl-jpa:5.0.0:jakarta")
	implementation("org.flywaydb:flyway-core")
	kapt("com.querydsl:querydsl-apt:5.0.0:jakarta")
	implementation("org.springframework.boot:spring-boot-starter-mail")

	// 기존 것 제거하고 이것만 추가
	implementation("io.github.resilience4j:resilience4j-spring-boot3:2.1.0")


	// OpenSearch Java Client (버전 명시 필수	)
	implementation("org.opensearch.client:opensearch-java:2.13.0")
	implementation("org.opensearch.client:opensearch-rest-client:2.13.0")
	implementation("com.fasterxml.jackson.core:jackson-databind")
	implementation("com.vladmihalcea:hibernate-types-60:2.21.1")

	implementation("io.hypersistence:hypersistence-utils-hibernate-60:3.7.3")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation("io.jsonwebtoken:jjwt-api:0.12.5")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

	runtimeOnly("org.postgresql:postgresql")
	testImplementation("com.h2database:h2")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("io.mockk:mockk:1.13.13")
	testImplementation("com.ninja-squad:springmockk:4.0.2")
	testImplementation("org.springframework.kafka:spring-kafka-test")
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:postgresql")
	testImplementation("org.testcontainers:testcontainers")
}

kotlin {
	jvmToolchain(21)
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

