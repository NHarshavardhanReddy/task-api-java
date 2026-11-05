# ---- build stage ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# ---- runtime stage ----
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/task-api-1.0.0.jar app.jar
ENV SPRING_DATASOURCE_URL=jdbc:sqlite:/data/tasks.db
RUN mkdir -p /data
EXPOSE 8081
# Render/Railway/Fly inject $PORT (read via server.port=${PORT:8081})
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "app.jar"]
