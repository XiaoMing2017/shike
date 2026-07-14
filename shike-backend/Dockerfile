# Stage 1: Build stage
FROM maven:3.8.8-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY settings.xml .
# Download dependencies using Aliyun mirror
RUN mvn dependency:go-offline -B --settings settings.xml
COPY src ./src
RUN mvn package -DskipTests --settings settings.xml

# Stage 2: Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/shike-backend-1.0.0.jar app.jar
# JVM optimization (optimized for 2-core 4GB server)
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8"
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
