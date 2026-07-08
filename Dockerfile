FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd pom.xml ./

RUN chmod +x mvnw

RUN ./mvnw -DskipTests dependency:go-offline

COPY src/ src/

RUN ./mvnw -DskipTests package

FROM mcr.microsoft.com/playwright/java:v1.61.0-jammy

WORKDIR /app

ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
ENV JAVA_OPTS="-XX:+UseSerialGC -XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/./urandom"

COPY --from=build /workspace/target/gate-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
