# Spring API + Vaadin admin. Build context: repository root (see .dockerignore).
# The jar is CPU-independent, so compile on the build machine's platform and only make the runtime
# stage match the target (e.g. docker buildx build --platform linux/amd64 on Apple Silicon).
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml package.json package-lock.json vite.config.ts tsconfig.json ./
COPY src ./src
RUN mvn -B -Pproduction -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 tenantsphere \
    && useradd --uid 10001 --gid tenantsphere --no-create-home tenantsphere \
    && mkdir -p /app/media/avatars \
    && chown -R 10001:10001 /app
COPY --from=build --chown=10001:10001 /build/target/tenant-sphere-0.1.0.jar /app/app.jar
USER 10001:10001
EXPOSE 8000
# Mail readiness is not app health: an SMTP outage must not mark the API container unhealthy.
ENV MANAGEMENT_HEALTH_MAIL_ENABLED=false
HEALTHCHECK --interval=60s --timeout=10s --start-period=300s --retries=6 \
    CMD curl --fail --silent --max-time 8 http://127.0.0.1:8000/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
