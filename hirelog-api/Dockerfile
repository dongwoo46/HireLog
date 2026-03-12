# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
RUN chmod +x gradlew

# 의존성 레이어 분리 + BuildKit 캐시로 재다운로드 방지
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew dependencies --no-daemon

COPY src src
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootJar -x test -x jar --no-daemon

# 레이어드 JAR 분해
RUN java -Djarmode=layertools -jar build/libs/hirelog-api-*.jar extract --destination extracted


# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S hirelog && adduser -S -G hirelog hirelog

# 레이어 순서대로 복사 (변경 빈도 낮은 것 → 높은 것)
COPY --chown=hirelog:hirelog --from=builder /app/extracted/dependencies ./
COPY --chown=hirelog:hirelog --from=builder /app/extracted/spring-boot-loader ./
COPY --chown=hirelog:hirelog --from=builder /app/extracted/snapshot-dependencies ./
COPY --chown=hirelog:hirelog --from=builder /app/extracted/application ./

USER hirelog

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "org.springframework.boot.loader.launch.JarLauncher"]
