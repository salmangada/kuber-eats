# Stage 1: Build
FROM eclipse-temurin:21 AS build

# Set working directory
WORKDIR /build

# Copy Maven wrapper & config from project root
COPY .mvn .mvn
COPY mvnw pom.xml ./

# Copy module pom
COPY order-service/pom.xml order-service/

# Download dependencies
RUN chmod +x mvnw && ./mvnw -B -f order-service/pom.xml dependency:go-offline

# Copy source code
COPY order-service/src order-service/src

# Build module
RUN ./mvnw -B -f order-service/pom.xml package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /order-service

# Copy jar from build stage
COPY --from=build /build/order-service/target/*.jar order-service.jar

ENTRYPOINT ["java", "-jar", "order-service.jar"]
