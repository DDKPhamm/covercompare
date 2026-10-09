# syntax=docker/dockerfile:1
#
# One Dockerfile for every service. Choose which with the SERVICE build argument, for example:
#   docker build --build-arg SERVICE=quote-service -t covercompare/quote-service .

# ---- Build stage: compile the chosen service and split its jar into layers ----
FROM eclipse-temurin:21-jdk-alpine AS build
ARG SERVICE
RUN test -n "$SERVICE" || (echo "Set the SERVICE build argument" && exit 1)
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY contracts/ contracts/
COPY pricing-service/ pricing-service/
COPY insurer-simulator/ insurer-simulator/
COPY quote-service/ quote-service/

# Tests already ran in the CI build job; this stage only packages. The cache mount keeps
# downloaded dependencies between builds without baking them into an image layer. It is locked
# because Compose builds all three images at once and Maven's cache is not safe for parallel writers.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    chmod +x mvnw && \
    ./mvnw --batch-mode --no-transfer-progress --projects "$SERVICE" --also-make package -DskipTests

# Dependencies change rarely and the application changes often, so they go in separate layers:
# a code change then only rebuilds and re-pushes the small application layer.
RUN java -Djarmode=tools -jar "$SERVICE/target/$SERVICE.jar" extract --layers \
    --application-filename app.jar --destination /workspace/extracted

# ---- Runtime stage: a JRE only, no compiler, no build tools, no source code ----
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER app

# Size the heap from the container's memory limit rather than the host's, and exit on
# OutOfMemoryError so the orchestrator restarts a clean process.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java", "-jar", "app.jar"]
