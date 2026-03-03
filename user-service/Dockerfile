# ─── STAGE 1: BUILD ──────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew

RUN ./gradlew dependencies --no-daemon

COPY src/ src/

RUN ./gradlew bootJar --no-daemon -x test --stacktrace

# ─── STAGE 2: RUNTIME ────────────────────────────────────
FROM amazoncorretto:25-alpine

WORKDIR /app

COPY --from=build /app/build/libs/user-service.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]