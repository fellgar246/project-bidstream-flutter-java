# Multi-stage build for BidStream API (< 300 MB, non-root)

FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
COPY backend/gradlew backend/gradle backend/settings.gradle.kts backend/build.gradle.kts backend/gradle.properties ./backend/
COPY backend/domain ./backend/domain
COPY backend/application ./backend/application
COPY backend/infrastructure ./backend/infrastructure
COPY backend/api ./backend/api
COPY backend/config ./backend/config
RUN cd backend && ./gradlew :api:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S bidstream && adduser -S bidstream -G bidstream
WORKDIR /app
COPY --from=builder /workspace/backend/api/build/libs/*.jar app.jar
USER bidstream
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --retries=10 \
  CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
