# Build stage: compile and package the Spring Boot application
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Runtime stage: only the JRE and the application jar
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --create-home shoplite
COPY --from=build /workspace/target/*.jar app.jar
USER shoplite
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
