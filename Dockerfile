FROM openjdk:17-jdk-slim

WORKDIR /app

// Será alterado para correção do nome, posteriormente
COPY target/seu-app.jar /app/seu-app.jar

EXPOSE 8080

// Será alterado para correção do nome, posteriormente
CMD ["java", "-jar", "seu-app.jar"]
