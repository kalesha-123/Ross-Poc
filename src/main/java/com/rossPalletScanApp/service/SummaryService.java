package com.rossPalletScanApp.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
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

            CellStyle titleStyle = wb.createCellStyle();
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            // Create sheet
            Sheet sheet = wb.createSheet("Receiving Worksheet");
            setupA4Print(sheet);

            // Title row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Receiving Worksheet of Pallet");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

            // Header row
            String[] cols = {"Appt", "PO", "SKU", "Description", "Style", "Color", "MLP", "LPN"};
            Row header = sheet.createRow(2);
            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIdx = 3;
            for (Map<String, Object> pallet : groups) {
                String mlp = safeStr(pallet.get("masterContainerId"));
                List<Map<String, Object>> boxes = safeListMap(pallet.get("boxes"));
                for (Map<String, Object> box : boxes) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(328); // Appt constant
                    row.createCell(1).setCellValue(safeStr(box.get("rossPo")));
                    row.createCell(2).setCellValue(safeStr(box.get("rossSkuNumber")));
                    row.createCell(3).setCellValue(safeStr(box.get("itemDescription")));
                    row.createCell(4).setCellValue(safeStr(box.get("rossStyle")));
                    row.createCell(5).setCellValue(safeStr(box.get("color")));
                    row.createCell(6).setCellValue(mlp);
                    row.createCell(7).setCellValue(safeStr(box.get("containerId")));
                }
            }

            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            // Output
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            byte[] bytes = out.toByteArray();

            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "receiving_worksheet_" + ts + ".xlsx";

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