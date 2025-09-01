FROM amazoncorretto:21
LABEL description="Smooth-UserService"
EXPOSE 8080
COPY build/libs/*.jar app.jar
CMD ["java", "-jar", "/app.jar"]
