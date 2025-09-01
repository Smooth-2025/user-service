FROM eclipse-temurin:21-jre-alpine
LABEL description="Docker image for user service"
EXPOSE 8080
COPY build/libs/smooth-backend-user-0.0.1-SNAPSHOT.jar app.jar
CMD ["java", "-jar", "/app.jar"]
