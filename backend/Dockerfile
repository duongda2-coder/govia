# Build stage: bien dich toan bo reactor (govia-core + govia-identity), chi lay jar cuoi cung.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -q -pl govia-identity -am package -DskipTests

# Run stage: chi JRE + jar da build, khong mang theo Maven/source de image nho gon.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/govia-identity/target/govia-identity.jar app.jar

# Render (va cac host khac) inject bien PORT - server.port trong application.yml da doc bien nay.
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
