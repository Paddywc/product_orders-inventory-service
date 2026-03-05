# syntax=docker/dockerfile:1

# -------- Build stage --------
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app

# Copy Maven wrapper and pom first (better layer caching)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies
RUN ./mvnw -q -DskipTests dependency:go-offline

# Copy source and build
COPY src/ src/
RUN ./mvnw -q -DskipTests package


# -------- Runtime stage --------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy built jar
COPY --from=build /app/target/*.jar /app/app.jar

# Inventory runs on 8086
EXPOSE 8086

ENTRYPOINT ["java","-jar","/app/app.jar"]