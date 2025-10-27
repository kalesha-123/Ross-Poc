package com.rossPalletScanApp.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.rossPalletScanApp.dto.ImagePreprocessor;
import com.rossPalletScanApp.dto.RossLabelExtractResponse;

@Service
public class ScanService {

    private final OcrService ocrService;
    private final RossLabelParser rossLabelParser;

    public ScanService(OcrService ocrService, RossLabelParser rossLabelParser) {
        this.ocrService = ocrService;
        this.rossLabelParser = rossLabelParser;
    }
    /** API #1: OCR only, no barcode, no DB */
    public ResponseEntity<RossLabelExtractResponse> extractRossLabel(MultipartFile imageFile) throws Exception {
        if (imageFile == null || imageFile.isEmpty()) {
            return ResponseEntity.badRequest().body(RossLabelExtractResponse.of(
                    "ERROR", "No image provided", 0, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
            ));
        }

        File preprocessedImage = null;

        try {
            // 1) Preprocess image for OCR
            preprocessedImage = ImagePreprocessor.preprocessToTempFile(imageFile.getInputStream());

            // 2) Perform OCR on preprocessed image using InputStream
            OcrService.OcrResult ocr;
            try (InputStream preprocessedStream = new FileInputStream(preprocessedImage)) {
                ocr = ocrService.extract(preprocessedStream);
            }

            int confidence = ocr.getConfidence();
            String raw = ocr.getText();

            if (confidence < ocrService.minConfidence()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        RossLabelExtractResponse.of(
                                "ERROR",
                                "Image not clear, please re-scan with better lighting or focus",
                                confidence,
                                imageFile.getOriginalFilename(),
                                raw,
                                null, null, null, null, null, null, null, null, null,
                                null, null, null, null, null
                        )
                );
            }

            // 3) Parse ROSS label
            RossLabelParser.Parsed p = rossLabelParser.parse(raw);

            // 3a) Validate mandatory trio: rossPo, color, rossSkuNumber
            String rossPo = p.getRossPo();
            String color = p.getColor();
            String rossSkuNumber = p.getRossSkuNumber();

            if (isBlank(rossPo) && isBlank(color) && isBlank(rossSkuNumber)) {
                // If all 3 are missing, return an error
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    RossLabelExtractResponse.of(
                        "ERROR",
                        "Unable to extract PO, Color, and SKU from the image. Please re-scan with better lighting/focus, or ensure the full label is visible.",
                        confidence,
                        imageFile.getOriginalFilename(),
                        raw,
                        null, // rossPo
                        p.getRossStyle(),
                        p.getItemDescription(),
                        null, // color
                        null, // rossSkuNumber
                        p.getQuantity(),
                        p.getNetWeightKg(),
                        p.getGrossWeightKg(),
                        p.getMeasurement(),
                        p.getConsignedTo(),
                        p.getDeliverTo(),
                        p.getDeliverToAddress(),
                        p.getCountryOfOrigin(),
                        p.getCartonNo()
                    )
                );
            }

            // 4) Build response when validation passes (at least one of the trio present)
            RossLabelExtractResponse resp = RossLabelExtractResponse.of(
                    "SUCCESS", "Label extracted successfully",
                    confidence, imageFile.getOriginalFilename(), raw,
                    rossPo, p.getRossStyle(), p.getItemDescription(), color,
                    rossSkuNumber, p.getQuantity(), p.getNetWeightKg(), p.getGrossWeightKg(),
                    p.getMeasurement(), p.getConsignedTo(), p.getDeliverTo(), p.getDeliverToAddress(),
                    p.getCountryOfOrigin(), p.getCartonNo()
            );

            return ResponseEntity.ok(resp);

        } finally {
            // 5) Cleanup temp file
            if (preprocessedImage != null && preprocessedImage.exists()) {
                preprocessedImage.delete();
            }
        }
    }

    /** Utility to treat null/blank uniformly */
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

}
