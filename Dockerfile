# ============================================
# Multi-stage Dockerfile — telegram-challenge-bot
# Builds the fat JAR from source inside Docker.
# ============================================

# ---- Stage 1: Build ----
FROM --platform=linux/amd64 gradle:8.14-jdk21 AS builder

WORKDIR /build

# Dependency layer (cached)
COPY gradle.properties settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY app.version ./

RUN gradle dependencies --no-daemon || true

# Source + build
COPY src ./src

RUN gradle shadowJar --no-daemon

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.source="https://github.com/alelk/telegram-challenge-bot"
LABEL org.opencontainers.image.description="Telegram Challenge Bot"
LABEL org.opencontainers.image.licenses="MIT"

# Non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser  -u 1001 -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /build/build/libs/*-all.jar ./app.jar

RUN chown -R appuser:appgroup /app
USER appuser

ENV JAVA_OPTS="\
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseG1GC \
  -XX:+UseStringDeduplication \
  -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar \"$@\"", "--"]