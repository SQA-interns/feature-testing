# syntax=docker/dockerfile:1
FROM eclipse-temurin:21.0.10_7-jdk-alpine AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:resolve
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests \
    && cp target/registration-backend-*.jar /build/app.jar

FROM eclipse-temurin:21.0.10_7-jre-alpine
RUN addgroup -S app && adduser -S -G app app \
    && mkdir -p /data/json-copies && chown -R app:app /data
WORKDIR /app
COPY --from=build /build/app.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
