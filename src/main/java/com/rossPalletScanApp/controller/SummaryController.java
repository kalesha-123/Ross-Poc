// src/main/java/com/rossPalletScanApp/controller/SummaryController.java
package com.rossPalletScanApp.controller;

import com.rossPalletScanApp.service.SummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/summary")
public class SummaryController {

    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    /**
     * POST /api/summary/excel
     * Body: payload returned by GET /api/groups/pallets
     * Returns: XLSX file bytes (A4-friendly) with summary sheets.
     */
    @PostMapping(
        value = "/excel",
        produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )
    public ResponseEntity<byte[]> exportExcel(@RequestBody Map<String, Object> groupedPayload) {
        return summaryService.generateSummaryExcel(groupedPayload);
    }
}
