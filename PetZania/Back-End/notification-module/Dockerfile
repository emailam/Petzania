# Stage 1: Build with Maven
FROM maven:3.9.11-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
# Download dependencies
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Runtime with Alpine JRE
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/notification-module-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]