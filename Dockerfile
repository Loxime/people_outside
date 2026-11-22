# syntax=docker/dockerfile:1

# Build stage
FROM maven:3.9.12-eclipse-temurin-25 AS build

WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp clean verify \
    && cp target/people-outside-*.jar /build/app.jar

# Runtime stage
FROM eclipse-temurin:25-jre-noble

WORKDIR /app

COPY --from=build --chown=10001:10001 \
    /build/app.jar /app/app.jar

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
