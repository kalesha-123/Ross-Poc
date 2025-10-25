// src/main/java/com/rossPalletScanApp/service/SummaryService.java
package com.rossPalletScanApp.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SummaryService {

    @SuppressWarnings("unchecked")
    public ResponseEntity<byte[]> generateSummaryExcel(Map<String, Object> groupedPayload) {
        Object dataObj = groupedPayload.get("data");
        List<Map<String, Object>> groups =
                (dataObj instanceof List) ? (List<Map<String, Object>>) dataObj : List.of();

        try (Workbook wb = new XSSFWorkbook()) {
            // Styles
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Sheet 1: Pallets summary
            Sheet palletSheet = wb.createSheet("Pallets");
            setupA4Print(palletSheet);
            String[] palletCols = {
                    "Pallet ID", "Pallet Code", "Master Container ID",
                    "Capacity", "Box Count", "rossPo", "Color", "rossSkuNumber"
            };
            Row ph = palletSheet.createRow(0);
            for (int c = 0; c < palletCols.length; c++) {
                Cell cell = ph.createCell(c);
                cell.setCellValue(palletCols[c]);
                cell.setCellStyle(headerStyle);
            }

            int r = 1;
            for (Map<String, Object> g : groups) {
                Map<String, String> combo = safeMapString(g.get("combination"));
                Row row = palletSheet.createRow(r++);
                row.createCell(0).setCellValue(safeLong(g.get("palletId")));
                row.createCell(1).setCellValue(safeStr(g.get("palletCode")));
                row.createCell(2).setCellValue(safeStr(g.get("masterContainerId")));
                row.createCell(3).setCellValue(safeInt(g.get("capacity")));
                row.createCell(4).setCellValue(safeInt(g.get("boxCount")));
                row.createCell(5).setCellValue(safeStr(combo.get("rossPo")));
                row.createCell(6).setCellValue(safeStr(combo.get("color")));
                row.createCell(7).setCellValue(safeStr(combo.get("rossSkuNumber")));
            }
            for (int c = 0; c < palletCols.length; c++) palletSheet.autoSizeColumn(c);

            // Sheet 2: Boxes detail
            Sheet boxSheet = wb.createSheet("Boxes");
            setupA4Print(boxSheet);
            String[] boxCols = {
                    "Box ID", "Container ID", "Pallet ID", "Pallet Code",
                    "rossPo", "Color", "rossSkuNumber", "Carton No",
                    "Image", "OCR Confidence", "Style", "Qty",
                    "Net Kg", "Gross Kg", "Measurement", "Consigned To",
                    "Deliver To", "Deliver To Address", "Origin", "Created At"
            };
            Row bh = boxSheet.createRow(0);
            for (int c = 0; c < boxCols.length; c++) {
                Cell cell = bh.createCell(c);
                cell.setCellValue(boxCols[c]);
                cell.setCellStyle(headerStyle);
            }

            int br = 1;
            for (Map<String, Object> g : groups) {
                List<Map<String, Object>> boxes = safeListMap(g.get("boxes"));
                for (Map<String, Object> b : boxes) {
                    Row row = boxSheet.createRow(br++);
                    row.createCell(0).setCellValue(safeLong(b.get("id")));
                    row.createCell(1).setCellValue(safeStr(b.get("containerId")));
                    row.createCell(2).setCellValue(safeLong(b.get("palletId")));
                    row.createCell(3).setCellValue(safeStr(b.get("palletCode")));
                    row.createCell(4).setCellValue(safeStr(b.get("rossPo")));
                    row.createCell(5).setCellValue(safeStr(b.get("color")));
                    row.createCell(6).setCellValue(safeStr(b.get("rossSkuNumber")));
                    row.createCell(7).setCellValue(safeStr(b.get("cartonNo")));
                    row.createCell(8).setCellValue(safeStr(b.get("imageFilename")));
                    row.createCell(9).setCellValue(safeInt(b.get("ocrConfidence")));
                    row.createCell(10).setCellValue(safeStr(b.get("rossStyle")));
                    row.createCell(11).setCellValue(safeStr(b.get("quantity")));
                    row.createCell(12).setCellValue(safeStr(b.get("netWeightKg")));
                    row.createCell(13).setCellValue(safeStr(b.get("grossWeightKg")));
                    row.createCell(14).setCellValue(safeStr(b.get("measurement")));
                    row.createCell(15).setCellValue(safeStr(b.get("consignedTo")));
                    row.createCell(16).setCellValue(safeStr(b.get("deliverTo")));
                    row.createCell(17).setCellValue(safeStr(b.get("deliverToAddress")));
                    row.createCell(18).setCellValue(safeStr(b.get("countryOfOrigin")));
                    row.createCell(19).setCellValue(safeStr(b.get("createdAt")));
                }
            }
            for (int c = 0; c < boxCols.length; c++) boxSheet.autoSizeColumn(c);

            // Output
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            byte[] bytes = out.toByteArray();

            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "summary_" + ts + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);

        } catch (Exception ex) {
            byte[] msg = ("Failed to generate Excel: " + ex.getMessage()).getBytes();
            return ResponseEntity.badRequest()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                    .body(msg);
        }
    }

    private void setupA4Print(Sheet sheet) {
        PrintSetup ps = sheet.getPrintSetup();
        ps.setPaperSize(PrintSetup.A4_PAPERSIZE);
        ps.setLandscape(true);
        sheet.setFitToPage(true);
        sheet.setAutobreaks(true);
    }

    private String safeStr(Object o) { return (o == null) ? "" : String.valueOf(o); }
    private int safeInt(Object o) {
        try { return Integer.parseInt(String.valueOf(o)); }
        catch (Exception e) { return 0; }
    }
    private long safeLong(Object o) {
        try { return Long.parseLong(String.valueOf(o)); }
        catch (Exception e) { return 0L; }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> safeMapString(Object o) {
        return (o instanceof Map) ? (Map<String, String>) o : new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> safeListMap(Object o) {
        return (o instanceof List) ? (List<Map<String, Object>>) o : List.of();
    }
}