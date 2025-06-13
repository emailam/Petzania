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
RUN apt-get update && \
    apt-get install -y iputils-ping redis-tools && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*
COPY --from=builder /build/target/RegistrationModule-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]