FROM maven:3.9.11-eclipse-temurin-25-alpine AS build
WORKDIR /workspace

COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f backend/pom.xml -DskipTests package

FROM eclipse-temurin:25-jre-alpine

RUN addgroup -S lottery && adduser -S lottery -G lottery
WORKDIR /app

COPY --from=build /workspace/backend/target/lottery-backend-0.1.0-SNAPSHOT.jar /app/lottery-backend.jar

ENV LOTTERY_HTTP_PORT=8080
EXPOSE 8080

USER lottery
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/lottery-backend.jar"]
