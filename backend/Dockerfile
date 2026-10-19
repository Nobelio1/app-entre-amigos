# Etapa 1: compila el backend con el wrapper de Gradle del propio proyecto
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
# Descarga dependencias en una capa propia para aprovechar la caché de Docker
RUN ./gradlew --no-daemon dependencies > /dev/null
COPY src ./src
RUN ./gradlew --no-daemon installDist -x test

# Etapa 2: imagen liviana solo con el JRE y la distribución generada
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/install/entreamigos-backend ./
EXPOSE 8080
ENTRYPOINT ["./bin/entreamigos-backend"]
