FROM amazoncorretto:21

WORKDIR /app
COPY build/libs/kotlin-tracer-mcp-0.0.1.jar app.jar
COPY repositories/ repositories/

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
