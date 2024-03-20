# AdoptOpenJDK를 기본 이미지로 사용
FROM openjdk:17-ea-11-jdk-slim

# 작업 디렉터리 설정
WORKDIR /usr/src/app

# JAR 파일을 컨테이너로 복사
COPY build/libs/smilestock-0.0.1-SNAPSHOT.jar app.jar

# 애플리케이션 실행
CMD ["java", "-jar", "app.jar"]