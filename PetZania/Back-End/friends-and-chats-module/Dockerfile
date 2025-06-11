# Base image with Java 21
FROM openjdk:21-slim

# Working directory in container
WORKDIR /app

# the JAR file
COPY target/friends-and-chats-module-0.0.1-SNAPSHOT.jar app.jar

# port 8081 (from properties file)
EXPOSE 8081

# command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]