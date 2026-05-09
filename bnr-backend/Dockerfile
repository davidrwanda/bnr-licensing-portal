# Author: David NTAMAKEMWA
# syntax=docker/dockerfile:1

# ── Stage 1: download dependencies ───────────────────────────────────────────
# Separate layer so Docker caches it. Only invalidated when pom.xml changes.
FROM maven:3.9-eclipse-temurin-21-alpine AS deps

WORKDIR /build

COPY pom.xml .

# Download all dependencies into the local repo inside the image.
# This layer is cached by Docker as long as pom.xml does not change.
RUN mvn dependency:go-offline -q --batch-mode

# ── Stage 2: compile and package ─────────────────────────────────────────────
# Only runs the full compiler — all JARs are already in /root/.m2 from stage 1.
FROM deps AS builder

COPY src ./src

RUN mvn package -DskipTests -o --batch-mode -q

# ── Stage 3: minimal runtime image ───────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="David NTAMAKEMWA"

WORKDIR /app

RUN addgroup -S bnr && adduser -S bnr -G bnr && \
    mkdir -p /app/uploads && chown bnr:bnr /app/uploads

COPY --from=builder /build/target/licensing-portal-*.jar app.jar

USER bnr

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
