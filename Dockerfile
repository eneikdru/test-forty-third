# Stage 1: Build the frontend
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2: Build the Spring Boot backend
FROM maven:3.9-eclipse-temurin-21 AS backend-build
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline -B
COPY src ./src
# Note: Vite is configured to output directly to '../src/main/resources/static'
# which resolves to '/app/src/main/resources/static' because WORKDIR is '/app/frontend'
COPY --from=frontend-build /app/src/main/resources/static ./src/main/resources/static
RUN mvn clean package -DskipTests

# Stage 3: Serve the application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-build /app/target/test-forty-third-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
