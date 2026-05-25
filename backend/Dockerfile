# syntax=docker/dockerfile:1.7
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -q -DskipTests dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/voiceinput-pro-backend-1.0.0.jar app.jar
RUN mkdir -p /app/data/uploads /app/data/exports /app/logs
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
