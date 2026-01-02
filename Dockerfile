FROM eclipse-temurin:11-jre

WORKDIR /app

# Copy the shaded/fat JAR produced by Maven Shade
COPY target/*-shaded.jar /app/app.jar

EXPOSE 8080
ENV PORT=8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
