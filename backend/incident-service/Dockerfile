FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build
RUN apk add --no-cache maven

COPY pom.xml ./pom.xml
RUN mvn -q -N install -f pom.xml

COPY incident-service/pom.xml ./incident-service/pom.xml
COPY incident-service/src     ./incident-service/src
RUN cd incident-service && mvn -q package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/incident-service/target/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
