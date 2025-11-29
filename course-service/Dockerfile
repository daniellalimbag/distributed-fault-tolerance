FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/*.jar /app/app.jar
EXPOSE 8083 9092
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
