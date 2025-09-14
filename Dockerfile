FROM maven:3.8.7-jdk-17-slim AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM adoptopenjdk/openjdk17:jdk-slim
WORKDIR /app
COPY --from=build /app/target/centraldoalfabeto-0.0.1-SNAPSHOT.jar .
EXPOSE 8080
CMD ["java", "-jar", "centraldoalfabeto-0.0.1-SNAPSHOT.jar"]
