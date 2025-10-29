FROM openjdk:17-jdk-slim

# Install dependencies for Tesseract 5.x
RUN apt-get update && \
    apt-get install -y software-properties-common && \
    add-apt-repository ppa:alex-p/tesseract-ocr5 && \
    apt-get update && \
    apt-get install -y \
        tesseract-ocr \
        libtesseract-dev \
        libleptonica-dev && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# (Optional) Install English language data
RUN apt-get install -y tesseract-ocr-eng

# Verify Tesseract installation
RUN tesseract --version

# Copy your app
WORKDIR /app
COPY target/*.jar app.jar

# Expose port if your app runs an HTTP server
EXPOSE 8080

# Run your app
ENTRYPOINT ["java","-jar","app.jar"]
