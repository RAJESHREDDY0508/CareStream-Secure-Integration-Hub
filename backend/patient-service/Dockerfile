FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build
RUN apk add --no-cache maven

COPY pom.xml ./pom.xml
RUN mvn -q -N install -f pom.xml

COPY patient-service/pom.xml ./patient-service/pom.xml
COPY patient-service/src     ./patient-service/src
RUN cd patient-service && mvn -q package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/patient-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
