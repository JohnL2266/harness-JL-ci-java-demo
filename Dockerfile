FROM eclipse-temurin:11-jre
WORKDIR /app

# We always build to target/app.jar via the pom.xml finalName above
COPY target/app.jar app.jar

ENTRYPOINT ["java","-jar","app.jar"]
