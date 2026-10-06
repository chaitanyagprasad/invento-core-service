FROM eclipse-temurin:21-jre

WORKDIR /app

COPY build/libs/invento-core-service-0.0.1-SNAPSHOT.jar app.jar

USER 10001:10001

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
