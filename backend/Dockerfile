# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY pom.xml .
RUN apk add --no-cache maven \
  && mvn dependency:go-offline -B -q

COPY src ./src
RUN mvn package -DskipTests -B -q

# Run stage
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache wget
WORKDIR /app

RUN adduser -D -s /bin/sh app && chown -R app:app /app
USER app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 39081
ENTRYPOINT ["java", "-jar", "app.jar"]
