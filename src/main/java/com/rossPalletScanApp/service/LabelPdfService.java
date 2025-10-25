// src/main/java/com/rossPalletScanApp/service/LabelPdfService.java
package com.rossPalletScanApp.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.common.BitMatrix;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class LabelPdfService {

    @SuppressWarnings("unchecked")
    public ResponseEntity<byte[]> generateBarcodePdf(Map<String, Object> payload) {
        String masterContainerId = safeStr(payload.get("masterContainerId"));
        List<Map<String, Object>> boxes =
                (List<Map<String, Object>>) payload.getOrDefault("data", List.of());

        PDDocument doc = new PDDocument();
        PDPageContentStream cs = null;
        try {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            cs = new PDPageContentStream(doc, page);
            float margin = 36f; // 0.5 inch
            float x = margin;
            float y = page.getMediaBox().getHeight() - margin;

            // Title
            y -= 16;
            writeText(cs, "Pallet Labels", x, y, 14);
            y -= 20;

            // Pallet barcode
            if (!masterContainerId.isBlank()) {
                BufferedImage palletBarcode = code128(masterContainerId, 600, 120);
                PDImageXObject palletImg = LosslessFactory.createFromImage(doc, palletBarcode);
                y -= 140;
                cs.drawImage(palletImg, x, y, 400, 120);
                y -= 16;
                writeText(cs, "MASTER: " + masterContainerId, x, y, 10);
                y -= 24;
            }

            // Boxes
            for (Map<String, Object> b : boxes) {
                // New page when space is low
                if (y < 200) {
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);

                    y = page.getMediaBox().getHeight() - margin - 16;
                    writeText(cs, "Pallet Labels (cont.)", x, y, 14);
                    y -= 20;
                }

                String containerId = safeStr(b.get("containerId"));
                String rossPo = safeStr(b.get("rossPo"));
                String color = safeStr(b.get("color"));
                String sku = safeStr(b.get("rossSkuNumber"));
                String palletCode = safeStr(b.get("palletCode"));

                BufferedImage boxBarcode = code128(containerId, 600, 120);
                PDImageXObject boxImg = LosslessFactory.createFromImage(doc, boxBarcode);

                y -= 140;
                cs.drawImage(boxImg, x, y, 400, 120);
                y -= 16;
                writeText(cs,
                        "BOX: " + containerId +
                        "  |  PO: " + rossPo +
                        "  |  Color: " + color +
                        "  |  SKU: " + sku +
                        "  |  Pallet: " + palletCode,
                        x, y, 9);
                y -= 24;
            }

            if (cs != null) cs.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            byte[] bytes = out.toByteArray();

            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "labels_" + ts + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(bytes);

        } catch (Exception ex) {
            try { if (cs != null) cs.close(); } catch (Exception ignore) {}
            byte[] msg = ("Failed to generate barcode PDF: " + ex.getMessage()).getBytes();
            return ResponseEntity.badRequest()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                    .body(msg);
        } finally {
            try { doc.close(); } catch (Exception ignore) {}
        }
    }

    private BufferedImage code128(String text, int width, int height) throws WriterException {
        Code128Writer writer = new Code128Writer();
        BitMatrix matrix = writer.encode(text, BarcodeFormat.CODE_128, width, height);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    private void writeText(PDPageContentStream cs, String text, float x, float y, int fontSize) throws Exception {
        var font = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA;
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private String safeStr(Object o) { return (o == null) ? "" : String.valueOf(o); }
}
