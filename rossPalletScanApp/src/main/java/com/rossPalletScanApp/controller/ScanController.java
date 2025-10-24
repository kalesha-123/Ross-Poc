package com.rossPalletScanApp.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.rossPalletScanApp.dto.RossLabelExtractResponse;
import com.rossPalletScanApp.service.ScanService;

@RestController
@RequestMapping("/api")
public class ScanController {

	private final ScanService scanService;

	public ScanController(ScanService scanService) {
		this.scanService = scanService;
	}

	/** 1) Image Scan - OCR only, parse ROSS label, no DB, no barcode */
	@PostMapping(path = "/scans", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<RossLabelExtractResponse> scan(@RequestPart("image") MultipartFile image) throws Exception {
		return scanService.extractRossLabel(image);
	}

}
