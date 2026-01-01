# Build security-common and install to local Maven repository
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy security-common pom and source
COPY pom.xml .
COPY src ./src

# Build and install to local Maven repository
RUN mvn clean install -DskipTests
