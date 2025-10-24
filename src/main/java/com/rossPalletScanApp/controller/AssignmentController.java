// src/main/java/com/rossPalletScanApp/controller/AssignmentController.java
package com.rossPalletScanApp.controller;

import com.rossPalletScanApp.dto.AssignBoxRequest;
import com.rossPalletScanApp.service.AssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AssignmentController {

	private final AssignmentService assignmentService;

	public AssignmentController(AssignmentService assignmentService) {
		this.assignmentService = assignmentService;
	}

	// Assign box to pallet (Submit button)
	@PostMapping("/assignments")
	public ResponseEntity<Map<String, Object>> assign(@RequestBody AssignBoxRequest req) {
		return assignmentService.assignBoxToPallet(req);
	}

	// List boxes by pallet
	@GetMapping("/pallets/{palletId}")
	public ResponseEntity<Map<String, Object>> boxesByPallet(@PathVariable Long palletId) {
		return assignmentService.listBoxesByPallet(palletId);
	}

	/**
	 * GET /api/groups/pallets Returns all boxes grouped by pallet.
	 */
	@GetMapping("/pallets")
	public ResponseEntity<Map<String, Object>> listAllGrouped() {
		return assignmentService.listAllBoxesGroupedByPallet();
	}

	// Delete box by box id and free containerId back to pool
	@DeleteMapping("/boxes/{boxId}")
	public ResponseEntity<Map<String, Object>> deleteBox(@PathVariable Long boxId) {
		return assignmentService.deleteBox(boxId);
	}

	// bulk delete boxes by pallet id (free containerIds back to pool)
	@DeleteMapping("/pallets/{palletId}")
	public ResponseEntity<Map<String, Object>> deleteByPallet(@PathVariable Long palletId) {
		return assignmentService.deleteBoxesByPallet(palletId);
	}

}