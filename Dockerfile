FROM eclipse-temurin:11-jre

WORKDIR /app

# Your pom.xml produces a stable jar name: target/app.jar
COPY target/app.jar /app/app.jar

EXPOSE 8080
ENV PORT=8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
