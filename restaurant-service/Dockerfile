# Stage 1: Build
FROM eclipse-temurin:21 AS build

# Set working directory
WORKDIR /build

# Copy Maven wrapper & config from project root
COPY .mvn .mvn
COPY mvnw pom.xml ./

# Copy module pom
COPY restaurant-service/pom.xml restaurant-service/

# Download dependencies
RUN chmod +x mvnw && ./mvnw -B -f restaurant-service/pom.xml dependency:go-offline

# Copy source code
COPY restaurant-service/src restaurant-service/src

# Build module
RUN ./mvnw -B -f restaurant-service/pom.xml package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /restaurant-service

# Copy jar from build stage
COPY --from=build /build/restaurant-service/target/*.jar restaurant-service.jar

ENTRYPOINT ["java", "-jar", "restaurant-service.jar"]
