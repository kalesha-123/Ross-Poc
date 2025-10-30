FROM ubuntu:24.04

# Install prerequisites
RUN apt-get update && apt-get install -y software-properties-common

# Add the official Tesseract OCR PPA (contains 5.5.1)
RUN add-apt-repository ppa:alex-p/tesseract-ocr-devel -y && \
    apt-get update && \
    apt-get install -y tesseract-ocr libtesseract-dev openjdk-17-jdk && \
    rm -rf /var/lib/apt/lists/*

# Verify installed version (optional)
RUN tesseract --version

# Set Java environment
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Set tessdata path
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/5/tessdata

# Set working directory
WORKDIR /app

# Copy JAR from build context
COPY target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java","-jar","/app/app.jar"]
