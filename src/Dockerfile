# Use official Java image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy jar file from target folder
COPY target/*.jar app.jar

# Run the Spring Boot app
ENTRYPOINT ["java","-jar","/app/app.jar"]
