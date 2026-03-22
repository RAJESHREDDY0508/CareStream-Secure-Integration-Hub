FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build
RUN apk add --no-cache maven

# Install parent POM into local Maven repo first
COPY pom.xml ./pom.xml
RUN mvn -q -N install -f pom.xml

# Build service
COPY auth-service/pom.xml ./auth-service/pom.xml
COPY auth-service/src     ./auth-service/src
RUN cd auth-service && mvn -q package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/auth-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
