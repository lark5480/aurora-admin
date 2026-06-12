# ── Stage 1: Build ──
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
# 先复制 pom，利用 Docker 缓存依赖层
COPY pom.xml .
RUN mvn dependency:go-offline -B
# 再复制源码并打包
COPY src ./src
RUN mvn package -DskipTests -B

# ── Stage 2: Runtime ──
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
