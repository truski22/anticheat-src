# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Copy library POMs first for layer caching
COPY anticheat-backend-libs/pom.xml anticheat-backend-libs/pom.xml
COPY anticheat-backend-libs/protocol/pom.xml anticheat-backend-libs/protocol/pom.xml
COPY anticheat-backend-libs/grpc-api/pom.xml anticheat-backend-libs/grpc-api/pom.xml
COPY anticheat-backend-gateway/pom.xml anticheat-backend-gateway/pom.xml

# Copy sources
COPY anticheat-backend-libs/protocol/src/ anticheat-backend-libs/protocol/src/
COPY anticheat-backend-libs/grpc-api/src/ anticheat-backend-libs/grpc-api/src/
COPY anticheat-backend-gateway/src/ anticheat-backend-gateway/src/

# Build libs then service
RUN mvn -f anticheat-backend-libs/pom.xml install -q -DskipTests && \
    mvn -f anticheat-backend-gateway/pom.xml package -q -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /build/anticheat-backend-gateway/target/gateway-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080 8081
CMD ["java", "-jar", "app.jar"]
