# Runtime image only: the jar is packaged first with ./mvnw -B package (specification S8).
FROM eclipse-temurin:21.0.10_7-jre-alpine
RUN addgroup -S -g 10001 app \
    && adduser -S -u 10001 -G app -H app \
    && mkdir -p /data/json-copies \
    && chown app:app /data/json-copies
WORKDIR /app
COPY target/registration-backend-0.1.0.jar /app/app.jar
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=6 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
