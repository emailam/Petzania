# Stage 1: Build with Maven
FROM maven:latest AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Runtime with JAR
FROM openjdk:21-slim
WORKDIR /app
COPY --from=builder /build/target/friends-and-chats-module-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]