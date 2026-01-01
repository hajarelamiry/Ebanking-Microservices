# Stage 1: Build security-common and crypto-service
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy security-common first and build it
COPY security-common/pom.xml ./security-common/pom.xml
COPY security-common/src ./security-common/src
WORKDIR /app/security-common
RUN mvn clean install -DskipTests

# Copy crypto-service and build it
WORKDIR /app
COPY crypto-service/pom.xml ./crypto-service/pom.xml
COPY crypto-service/src ./crypto-service/src
WORKDIR /app/crypto-service
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache wget
WORKDIR /app

# Copy the built JAR
COPY --from=build /app/crypto-service/target/*.jar app.jar

# Expose the port
EXPOSE 8085

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8085/actuator/health 2>/dev/null || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
