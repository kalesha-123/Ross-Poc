FROM debian:bookworm-slim

# Install OpenJDK 17 and Tesseract 5.x (already available in bookworm)
RUN apt-get update && \
    apt-get install -y openjdk-17-jdk tesseract-ocr libtesseract-dev && \
    rm -rf /var/lib/apt/lists/*

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