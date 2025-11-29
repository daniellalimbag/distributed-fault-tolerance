FROM eclipse-temurin:21-jre
WORKDIR /app

# Expect the jar to be prebuilt on the host: :web-node:bootJar
COPY web-node/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENV JAVA_OPTS=""
# Allow overriding API address to point to api-node service
ENV API_ADDRESS="static://api-node:9090"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
