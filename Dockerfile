FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/centraldoalfabeto-0.0.1-SNAPSHOT.jar /app/centraldoalfabeto-0.0.1-SNAPSHOT.jar

EXPOSE 8080

CMD ["java", "-jar", "centraldoalfabeto-0.0.1-SNAPSHOT.jar"]
