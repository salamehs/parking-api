# ---- build: tests are run by the CI, the image build only packages ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml lombok.config ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- runtime: JRE only, non-root user ----
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 --no-create-home app
WORKDIR /app
COPY --from=build /build/target/parking-api-*.jar app.jar
USER app
EXPOSE 8080
# container-aware heap sizing; secrets and URLs come from environment variables
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
# no HEALTHCHECK: the JRE image has neither curl nor wget; let the orchestrator probe /actuator/health
ENTRYPOINT ["java", "-jar", "app.jar"]
