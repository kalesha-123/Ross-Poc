// src/main/java/com/rossPalletScanApp/controller/LabelController.java
package com.rossPalletScanApp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.rossPalletScanApp.service.LabelPdfService;

import java.util.Map;

@RestController
@RequestMapping("/api/labels")
public class LabelController {

    private final LabelPdfService labelPdfService;

    public LabelController(LabelPdfService labelPdfService) {
        this.labelPdfService = labelPdfService;
    }

    /**
     * POST /api/labels/barcode
     * Body: payload returned by GET /api/pallets/{palletId}/boxes plus masterContainerId
     * Returns: PDF (A4) with barcodes for pallet & each box.
     */
    @PostMapping("/barcode")
    public ResponseEntity<byte[]> generatePalletLabels(@RequestBody Map<String, Object> palletBoxesPayload) {
        return labelPdfService.generateBarcodePdf(palletBoxesPayload);
    }
}