package com.rossPalletScanApp.controller;

import org.springframework.web.bind.annotation.*;

import com.rossPalletScanApp.dto.ApiResponse;
import com.rossPalletScanApp.dto.OcrLabelDto;
import com.rossPalletScanApp.dto.PalletAvailabilityDto;
import com.rossPalletScanApp.entity.Pallet;
import com.rossPalletScanApp.repository.PalletRepository;
import com.rossPalletScanApp.service.PalletService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pallets")
public class PalletController {

	private final PalletService palletService;
	private final PalletRepository palletRepository;

	public PalletController(PalletService palletService, PalletRepository palletRepository) {
		this.palletService = palletService;
		this.palletRepository = palletRepository;
	}

	/**
	 * 2nd API: Check pallet availability for a combination. Input: OCR payload
	 * (subset used: rossPo, color, rossSkuNumber)
	 */
	@PostMapping("/availability")
	public ApiResponse<PalletAvailabilityDto> availability(@RequestBody OcrLabelDto ocr) {
		List<PalletAvailabilityDto> items = palletService.checkAvailability(ocr);
		int total = palletRepository.findAll().size();
		int valid = (int) items.stream().filter(i -> i.canAccept).count();

		Map<String, String> combo = new HashMap<>();
		combo.put("rossPo", safe(ocr.rossPo));
		combo.put("color", safe(ocr.color));
		combo.put("rossSkuNumber", safe(ocr.rossSkuNumber));

		ApiResponse<PalletAvailabilityDto> r = ApiResponse.of("SUCCESS", "Pallet availability fetched", items);
		r.totalPallets = total;
		r.validPalletCount = valid;
		r.requestedCombination = combo;
		return r;
	}

	private String safe(String s) {
		return (s == null) ? "" : s.trim();
	}
}