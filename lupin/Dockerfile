# Build stage
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy gradle files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Download dependencies
RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

# Copy source code and build
COPY src src

# [추가] React public 폴더의 이미지들을 Spring Boot 정적 리소스 경로(static)로 복사
# 이렇게 해야 https://도메인/icon-report.webp 로 접근했을 때 파일이 보입니다.
COPY public src/main/resources/static

RUN ./gradlew build -x test --no-daemon

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy jar file
COPY --from=build /app/build/libs/*.jar app.jar

# Set timezone
ENV TZ=Asia/Seoul
RUN apk add --no-cache tzdata

# Expose port
EXPOSE 8081

# Run application
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
