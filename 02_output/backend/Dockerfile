# Runtime image of the backend; the jar is built first with ./mvnw package (see README).
FROM eclipse-temurin:21.0.10_7-jre-alpine

RUN addgroup -S -g 10001 app \
    && adduser -S -D -H -u 10001 -G app app \
    && mkdir -p /app /data/json-copies \
    && chown -R app:app /data

WORKDIR /app
COPY --chown=app:app target/registration-backend-1.0.0.jar app.jar

USER app:app
VOLUME ["/data/json-copies"]
EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health/readiness > /dev/null || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
