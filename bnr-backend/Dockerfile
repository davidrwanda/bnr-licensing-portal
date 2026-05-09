# Author: David NTAMAKEMWA

# ── Stage 1: dependency cache ─────────────────────────────────────────────────
# Use the official Maven + JDK 21 image — no apk install needed.
FROM maven:3.9-eclipse-temurin-21-alpine AS deps

WORKDIR /build

# Copy only the POM first so this layer is cached as long as dependencies
# don't change. Source changes won't invalidate this layer.
COPY pom.xml .
RUN mvn dependency:go-offline -q

# ── Stage 2: compile and package ─────────────────────────────────────────────
FROM deps AS builder

COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 3: minimal runtime image ───────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="David NTAMAKEMWA"

WORKDIR /app

# Non-root user for security
RUN addgroup -S bnr && adduser -S bnr -G bnr && \
    mkdir -p /app/uploads && chown bnr:bnr /app/uploads

COPY --from=builder /build/target/licensing-portal-*.jar app.jar

USER bnr

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
