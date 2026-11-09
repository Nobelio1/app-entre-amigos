FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
# Normaliza finales de línea (CRLF en checkouts de Windows) y asegura permiso de ejecución
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew \
    && ./gradlew --no-daemon dependencies > /dev/null
COPY src ./src
RUN ./gradlew --no-daemon installDist -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/install/entreamigos-backend ./
EXPOSE 8080
ENTRYPOINT ["./bin/entreamigos-backend"]
