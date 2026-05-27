# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

COPY anticheat-backend-libs/pom.xml chessfraud-libs/pom.xml
COPY anticheat-backend-libs/protocol/pom.xml chessfraud-libs/protocol/pom.xml
COPY anticheat-backend-libs/mqtt-lib/pom.xml chessfraud-libs/mqtt-lib/pom.xml
COPY anticheat-backend-libs/ws-lib/pom.xml chessfraud-libs/ws-lib/pom.xml
COPY anticheat-backend-user-service/pom.xml user-service/pom.xml

COPY anticheat-backend-libs/protocol/src/ chessfraud-libs/protocol/src/
COPY anticheat-backend-libs/mqtt-lib/src/ chessfraud-libs/mqtt-lib/src/
COPY anticheat-backend-libs/ws-lib/src/ chessfraud-libs/ws-lib/src/
COPY anticheat-backend-user-service/src/ user-service/src/

RUN mvn -f chessfraud-libs/pom.xml install -q -DskipTests && \
    mvn -f user-service/pom.xml package -q -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /build/user-service/target/user-service-1.0-SNAPSHOT.jar app.jar
COPY anticheat-backend-libs/protocol/src/main/configuration/ chessfraud-libs/protocol/src/main/configuration/
COPY anticheat-backend-libs/protocol/src/main/images/ chessfraud-libs/protocol/src/main/images/

CMD ["java", "-jar", "app.jar"]
