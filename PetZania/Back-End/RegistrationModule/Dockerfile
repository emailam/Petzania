# Base image with Java 21
FROM openjdk:21-slim

# Working directory in container
WORKDIR /app

# The jar file
COPY target/RegistrationModule-0.0.1-SNAPSHOT.jar app.jar

# port 8080 (from properties file)
EXPOSE 8080

# command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
