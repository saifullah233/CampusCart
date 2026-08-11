# syntax=docker/dockerfile:1
# --- Build stage -------------------------------------------------------------
# Pinned to JDK 21 for production parity regardless of the local build JDK.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache dependencies first for faster incremental builds (layer is reused while
# pom.xml is unchanged).
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
# Tests run in CI against Testcontainers; the image build itself skips them so it
# does not require a Docker-in-Docker daemon.
RUN mvn -q -B clean package -DskipTests

# --- Runtime stage -----------------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# curl is used by the container HEALTHCHECK against the actuator health endpoint.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system campuscart \
    && useradd --system --gid campuscart --home /app campuscart

# Copy the fat jar with non-root ownership.
COPY --from=build --chown=campuscart:campuscart /workspace/target/campuscart-backend.jar app.jar

USER campuscart

# Respect container memory limits; override or extend via JAVA_OPTS at runtime.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

# exec form keeps the JVM as PID 1 so it receives SIGTERM for graceful shutdown.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
