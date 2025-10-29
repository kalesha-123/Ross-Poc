FROM openjdk:17-jdk-jammy

# Install Tesseract 5.x and dependencies
RUN apt-get update && \
    apt-get install -y software-properties-common gnupg && \
    add-apt-repository ppa:alex-p/tesseract-ocr5 && \
    apt-get update && \
    apt-get install -y \
        tesseract-ocr \
        libtesseract-dev \
        libleptonica-dev \
        tesseract-ocr-eng && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Verify version
RUN tesseract --version

# Copy your app
WORKDIR /app
COPY target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
