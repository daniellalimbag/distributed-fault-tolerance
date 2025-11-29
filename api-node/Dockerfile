FROM eclipse-temurin:21-jre
WORKDIR /app

# Expect the jar to be prebuilt on the host: :api-node:bootJar
COPY api-node/build/libs/*.jar /app/app.jar

EXPOSE 8081 9090
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
