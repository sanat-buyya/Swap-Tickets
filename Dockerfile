# ===============================
# 1. Use official OpenJDK image
# ===============================
FROM openjdk:17-jdk-slim

# ===============================
# 2. Set working directory
# ===============================
WORKDIR /app

# ===============================
# 3. Copy Maven build output JAR
# ===============================
COPY target/SwapTicket-0.0.1-SNAPSHOT.jar app.jar

# ===============================
# 4. Expose application port
# ===============================
EXPOSE 8085

# ===============================
# 5. Run the Spring Boot JAR
# ===============================
ENTRYPOINT ["java", "-jar", "app.jar"]
