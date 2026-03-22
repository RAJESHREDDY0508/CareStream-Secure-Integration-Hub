FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build
RUN apk add --no-cache maven

COPY pom.xml ./pom.xml
RUN mvn -q -N install -f pom.xml

COPY api-gateway/pom.xml ./api-gateway/pom.xml
COPY api-gateway/src     ./api-gateway/src
RUN cd api-gateway && mvn -q package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
