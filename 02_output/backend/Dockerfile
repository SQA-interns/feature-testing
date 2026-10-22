# Backend image: build the jar first with ./mvnw package (see README)
FROM eclipse-temurin:21.0.10_7-jre-alpine

RUN addgroup -S app && adduser -S -G app app \
    && mkdir -p /data/json-copies /app \
    && chown -R app:app /data /app

WORKDIR /app
COPY --chown=app:app target/registration-backend-*.jar /app/app.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
