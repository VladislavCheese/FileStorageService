FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-Dfilestorage.base=/data","-jar","/app/app.jar"]