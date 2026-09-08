# syntax=docker/dockerfile:1
FROM node:24-bookworm-slim AS frontend
WORKDIR /build/frontend
COPY Frontend/package*.json ./
RUN npm ci
COPY Frontend/ ./
RUN npm run lint && npm test && npm run build

FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /build
COPY pom.xml ./
COPY src/ ./src/
COPY --from=frontend /build/frontend/dist/ ./src/main/resources/static/
RUN mvn -B -ntp package

FROM eclipse-temurin:21-jre-jammy AS runtime
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 nexo \
    && useradd --uid 10001 --gid nexo --no-create-home --shell /usr/sbin/nologin nexo
WORKDIR /app
COPY --from=backend --chown=nexo:nexo /build/target/CriptoHibrida-0.0.1-SNAPSHOT.jar ./app.jar
USER nexo
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65.0" \
    SERVER_PORT=8080 \
    SERVER_FORWARD_HEADERS_STRATEGY=framework \
    SERVER_SHUTDOWN=graceful
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl --fail --silent --output /dev/null http://127.0.0.1:8080/ || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
