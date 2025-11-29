# Build stage
FROM gradle:8.9.0-jdk21 AS build
WORKDIR /home/gradle/src

# Copy Gradle settings and project modules
COPY settings.gradle build.gradle ./
COPY api-node api-node
COPY web-node web-node

# Build only the web-node fat jar
RUN gradle :web-node:bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy built jar
COPY --from=build /home/gradle/src/web-node/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENV JAVA_OPTS=""
# Allow overriding API address to point to api-node service
ENV API_ADDRESS="static://api-node:9090"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
