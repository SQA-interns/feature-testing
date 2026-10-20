# Build stage: Temurin JDK of the pinned release (D-12)
FROM eclipse-temurin:21.0.10_7-jdk-alpine AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml dependency-check-suppressions.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:resolve
COPY src src
RUN ./mvnw -B -q -DskipTests package && cp target/registration-backend-1.0.0.jar /app.jar

# Runtime stage: JRE only, non-root user (SB-11)
FROM eclipse-temurin:21.0.10_7-jre-alpine
RUN addgroup -S -g 10001 app && adduser -S -D -H -u 10001 -G app app \
    && mkdir -p /app /data/json-copies && chown app:app /data/json-copies
COPY --from=build /app.jar /app/app.jar
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=10 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
