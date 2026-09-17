# Stage 1: Build stage
FROM maven:3.8.8-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/shike-backend-1.0.0.jar app.jar
ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_OPTS="-Xms128m -Xmx320m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
EXPOSE 8081 10000
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=${PORT:-8081}"]
