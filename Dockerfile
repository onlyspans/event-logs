# Build stage
FROM eclipse-temurin:25-jdk AS build

# Build arguments for versioning
ARG VERSION=dev
ARG REVISION=unknown

WORKDIR /app

# Copy gradle wrapper and configuration files
COPY gradlew gradlew.bat ./
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src src

# Build the application (skip tests for faster builds)
RUN ./gradlew bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:25-jre

# Pass build args to runtime stage
ARG VERSION=dev
ARG REVISION=unknown

WORKDIR /app

# Add labels for metadata
LABEL org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.revision="${REVISION}" \
      org.opencontainers.image.title="event-logs" \
      org.opencontainers.image.description="OnlySpans Event Logs Service"

# Create non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring && \
    chown -R spring:spring /app

# Copy the built JAR from build stage
COPY --from=build --chown=spring:spring /app/build/libs/*.jar app.jar

USER spring:spring

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

# Migrations stage
FROM flyway/flyway:10-alpine AS migrations

COPY src/main/resources/db/migration /flyway/sql

ENTRYPOINT ["flyway", "-connectRetries=60", "migrate"]
