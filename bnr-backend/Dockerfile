# --- Stage 1: build the fat JAR ---
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

COPY pom.xml .
COPY src ./src

# Download dependencies first (cached layer), then package
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -q && \
    mvn package -DskipTests -q

# --- Stage 2: minimal runtime image ---
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="David NTAMAKEMWA"

WORKDIR /app

# Non-root user for security
RUN addgroup -S bnr && adduser -S bnr -G bnr

COPY --from=builder /build/target/licensing-portal-*.jar app.jar

# Directory for uploaded files — mounted as a volume in docker-compose
RUN mkdir -p /app/uploads && chown bnr:bnr /app/uploads

USER bnr

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
