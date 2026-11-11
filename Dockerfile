# syntax=docker/dockerfile:1

# ---- Estágio de build ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /build

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q -DskipTests package

# ---- Imagem final ----
# Base Debian/Ubuntu (glibc): a biblioteca nativa do Vosk não funciona em Alpine (musl).
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --no-create-home --shell /usr/sbin/nologin appuser

COPY --from=build /build/target/centraldoalfabeto-0.0.1-SNAPSHOT.jar app.jar
# Modelo Vosk: lido de ./vosk-model em tempo de execução (ver download_vosk_model.sh).
# Nenhum .env é copiado: as variáveis são injetadas em tempo de execução.
COPY vosk-model vosk-model
RUN chown -R appuser /app

USER appuser
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=60"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
