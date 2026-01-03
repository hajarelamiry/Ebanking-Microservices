# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy security-common first and build it
COPY security-common/pom.xml ./security-common/pom.xml
COPY security-common/src ./security-common/src
WORKDIR /app/security-common
RUN mvn clean install -DskipTests

# Copy auth-service and build it
WORKDIR /app
COPY auth-service/pom.xml ./auth-service/pom.xml
COPY auth-service/src ./auth-service/src
WORKDIR /app/auth-service
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache wget
WORKDIR /app

# Copy the built JAR
COPY --from=build /app/auth-service/target/*.jar app.jar

# Expose the port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
