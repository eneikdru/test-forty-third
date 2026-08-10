# Use a lightweight JRE base image to minimize write layers and avoid filesystem whiteout errors during build
FROM eclipse-temurin:21-jre-alpine

# Set the working directory
WORKDIR /app

# Copy the pre-built jar from the host
COPY target/test-forty-third-0.0.1-SNAPSHOT.jar app.jar

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
