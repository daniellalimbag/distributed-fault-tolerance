# Build stage
FROM gradle:8.9.0-jdk21 AS build
WORKDIR /home/gradle/src

# Copy Gradle settings and project modules
COPY settings.gradle build.gradle ./
COPY api-node api-node
COPY web-node web-node

# Build only the api-node fat jar
RUN gradle :api-node:bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy built jar
COPY --from=build /home/gradle/src/api-node/build/libs/*.jar /app/app.jar

EXPOSE 8081 9090
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
