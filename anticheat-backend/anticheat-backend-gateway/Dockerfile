# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Copy library POMs first for layer caching
COPY anticheat-backend-libs/pom.xml chessfraud-libs/pom.xml
COPY anticheat-backend-libs/protocol/pom.xml chessfraud-libs/protocol/pom.xml
COPY anticheat-backend-libs/mqtt-lib/pom.xml chessfraud-libs/mqtt-lib/pom.xml
COPY anticheat-backend-libs/ws-lib/pom.xml chessfraud-libs/ws-lib/pom.xml
COPY anticheat-backend-gateway/pom.xml gateway/pom.xml

# Copy sources
COPY anticheat-backend-libs/protocol/src/ chessfraud-libs/protocol/src/
COPY anticheat-backend-libs/mqtt-lib/src/ chessfraud-libs/mqtt-lib/src/
COPY anticheat-backend-libs/ws-lib/src/ chessfraud-libs/ws-lib/src/
COPY anticheat-backend-gateway/src/ gateway/src/

# Build libs then service
RUN mvn -f chessfraud-libs/pom.xml install -q -DskipTests && \
    mvn -f gateway/pom.xml package -q -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /build/gateway/target/gateway-1.0-SNAPSHOT.jar app.jar
COPY anticheat-backend-libs/protocol/src/main/configuration/ chessfraud-libs/protocol/src/main/configuration/

EXPOSE 8080 8081
CMD ["java", "-jar", "app.jar"]
