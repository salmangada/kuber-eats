# Stage 1: Build
FROM eclipse-temurin:17 AS build

# Set working directory
WORKDIR /build

# Copy Maven wrapper & config from project root
COPY .mvn .mvn
COPY mvnw pom.xml ./

# Copy module pom
COPY user-service/pom.xml user-service/

# Download dependencies
RUN chmod +x mvnw && ./mvnw -B -f user-service/pom.xml dependency:go-offline

# Copy source code
COPY user-service/src user-service/src

# Build module
RUN ./mvnw -B -f user-service/pom.xml package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /user-service

# Copy jar from build stage
COPY --from=build /build/user-service/target/*.jar user-service.jar

ENTRYPOINT ["java", "-jar", "user-service.jar"]
