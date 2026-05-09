# Author: David NTAMAKEMWA
# syntax=docker/dockerfile:1

# ── Stage 1: build the fat JAR ────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

COPY pom.xml .
COPY src ./src

# --mount=type=cache persists the Maven local repository between builds on the
# same machine. First build still downloads everything; every subsequent build
# is a cache hit and takes ~20 s instead of ~3 min.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn package -DskipTests --no-transfer-progress -q

# ── Stage 2: minimal runtime image ───────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="David NTAMAKEMWA"

WORKDIR /app

RUN addgroup -S bnr && adduser -S bnr -G bnr && \
    mkdir -p /app/uploads && chown bnr:bnr /app/uploads

COPY --from=builder /build/target/licensing-portal-*.jar app.jar

USER bnr

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
