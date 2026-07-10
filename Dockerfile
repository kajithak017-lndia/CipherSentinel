# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Tesseract OCR (needed by OCRService)
RUN apt-get update && \
    apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-eng && \
    rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar

# Folder where uploaded documents are stored (see DocumentService uploadDir)
RUN mkdir -p /app/uploads

EXPOSE 8080

# Detect the installed tessdata path at startup (varies by base image/version)
# and export it before launching, instead of hardcoding a version number.
ENTRYPOINT ["/bin/bash", "-c", "export TESSDATA_PREFIX=$(find /usr -type d -name tessdata | head -n 1); java -jar app.jar"]