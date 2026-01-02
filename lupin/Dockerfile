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

# [수정] public 폴더 내용을 resources/static으로 확실하게 복사
# mkdir로 폴더를 먼저 만들어서 복사 오류 방지
RUN mkdir -p src/main/resources/static
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
