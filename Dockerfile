# --- Build stage -------------------------------------------------------------
# Pinned to JDK 21 for production parity regardless of the local build JDK.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache dependencies first for faster incremental builds.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B clean package -DskipTests

# --- Runtime stage -----------------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as a non-root user.
RUN groupadd --system campuscart && useradd --system --gid campuscart campuscart
USER campuscart

COPY --from=build /workspace/target/campuscart-backend.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
