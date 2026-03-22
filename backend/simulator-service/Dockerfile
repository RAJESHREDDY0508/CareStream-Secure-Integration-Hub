FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build
RUN apk add --no-cache maven

COPY pom.xml ./pom.xml
RUN mvn -q -N install -f pom.xml

COPY simulator-service/pom.xml ./simulator-service/pom.xml
COPY simulator-service/src     ./simulator-service/src
RUN cd simulator-service && mvn -q package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/simulator-service/target/*.jar app.jar
EXPOSE 8089
ENTRYPOINT ["java", "-jar", "app.jar"]
