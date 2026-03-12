# Stage 1: 레이어드 JAR 분해 (runner에서 빌드된 JAR 사용)
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

COPY build/libs/hirelog-api-*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract --destination extracted


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
