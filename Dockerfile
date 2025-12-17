FROM eclipse-temurin:17-jdk
MAINTAINER Traversium Developers
WORKDIR /opt/filestorage-service

COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/opt/filestorage-service/app.jar"]