# Stage 1: Build with Maven
FROM maven:3.9.11-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
# Download dependencies
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Runtime with JRE
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/target/adoption-and-breeding-module-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]