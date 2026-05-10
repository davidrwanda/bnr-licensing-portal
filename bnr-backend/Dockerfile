# Author: David NTAMAKEMWA
# syntax=docker/dockerfile:1

# ── Stage 1: build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build
COPY pom.xml .
RUN mvn dependency:resolve dependency:resolve-plugins -q --batch-mode
RUN mvn package -DskipTests -q --batch-mode

# ── Stage 2: minimal runtime ──────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="David NTAMAKEMWA"

WORKDIR /app

RUN addgroup -S bnr && adduser -S bnr -G bnr && \
    mkdir -p /app/uploads && chown bnr:bnr /app/uploads

COPY --from=builder /build/target/licensing-portal-*.jar app.jar

USER bnr

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
