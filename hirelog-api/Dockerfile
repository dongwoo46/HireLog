# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

COPY src src
RUN ./gradlew bootJar -x test -x jar --no-daemon

# 레이어드 JAR 분해
RUN java -Djarmode=layertools -jar build/libs/hirelog-api-*.jar extract --destination extracted


# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN groupadd -r hirelog && useradd -r -g hirelog hirelog

# 레이어 순서대로 복사 (변경 빈도 낮은 것 → 높은 것)
COPY --from=builder /app/extracted/dependencies ./
COPY --from=builder /app/extracted/spring-boot-loader ./
COPY --from=builder /app/extracted/snapshot-dependencies ./
COPY --from=builder /app/extracted/application ./

RUN chown -R hirelog:hirelog /app
USER hirelog

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "org.springframework.boot.loader.launch.JarLauncher"]