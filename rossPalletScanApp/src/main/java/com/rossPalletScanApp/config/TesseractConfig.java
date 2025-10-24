package com.rossPalletScanApp.config;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TesseractConfig {

@Value("${app.ocr.language:eng}")
private String language;

@Value("${app.ocr.tessdata-path:}")
private String tessdataPath;

@Bean
public Tesseract tesseract() {
 Tesseract t = new Tesseract();
 if (tessdataPath != null && !tessdataPath.isBlank()) {
   t.setDatapath(tessdataPath);
 }
 t.setLanguage(language);
 t.setTessVariable("user_defined_dpi", "300");
 t.setTessVariable("preserve_interword_spaces", "1");
 return t;
}
}