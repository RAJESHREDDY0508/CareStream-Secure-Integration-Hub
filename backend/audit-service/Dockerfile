FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build
RUN apk add --no-cache maven

COPY pom.xml ./pom.xml
RUN mvn -q -N install -f pom.xml

COPY audit-service/pom.xml ./audit-service/pom.xml
COPY audit-service/src     ./audit-service/src
RUN cd audit-service && mvn -q package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/audit-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
