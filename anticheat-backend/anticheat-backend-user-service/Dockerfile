# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Copy POM first for layer caching
COPY anticheat-backend-user-service/pom.xml pom.xml

# Copy sources
COPY anticheat-backend-user-service/src/ src/

RUN mvn package -q -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /build/target/user-service-1.0-SNAPSHOT.jar app.jar

EXPOSE 9090 9091
CMD ["java", "-jar", "app.jar"]
