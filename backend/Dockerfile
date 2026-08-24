# Build stage: bien dich toan bo reactor (govia-core + govia-identity), chi lay jar cuoi cung.
# Dung image co san Maven (khong qua mvnw) de build khong phu thuoc mang tai rieng Maven binary tu
# repo.maven.apache.org luc build - da tung gay build fail tren Render du file van ton tai binh
# thuong, chi la mang cua build server luc do khong toi duoc (loi tam thoi, khong lien quan code).
FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -pl govia-identity -am package -DskipTests

# Run stage: chi JRE + jar da build, khong mang theo Maven/source de image nho gon.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/govia-identity/target/govia-identity.jar app.jar

# Render (va cac host khac) inject bien PORT - server.port trong application.yml da doc bien nay.
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
