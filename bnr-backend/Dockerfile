# Author: David NTAMAKEMWA
# syntax=docker/dockerfile:1

# ── Stage 1: build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy pom.xml first and resolve all dependencies as a separate layer.
# Docker caches this layer — it is only invalidated when pom.xml changes.
# Source code changes do NOT invalidate the dependency download.
COPY pom.xml .
RUN mvn dependency:resolve dependency:resolve-plugins -q --batch-mode

# Now copy source and build. Maven finds all JARs already in /root/.m2.
COPY src ./src
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
