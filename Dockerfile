FROM openjdk:19-jdk-alpine AS build
WORKDIR /app
RUN apk add --no-cache maven
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:19-jre-alpine
WORKDIR /app
COPY --from=build /app/target/Peer2Peer-1.0-SNAPSHOT.jar app.jar
CMD ["java", "-jar", "app.jar"]