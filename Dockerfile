FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests
EXPOSE 8080
CMD ["java", "-jar", "target/centraldoalfabeto-0.0.1-SNAPSHOT.jar"]

ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV MAVEN_OPTS="-Dmaven.repo.local=.m2/repository"
