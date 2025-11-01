// src/main/java/com/rossPalletScanApp/service/AssignmentService.java
package com.rossPalletScanApp.service;

import com.rossPalletScanApp.dto.AssignBoxRequest;
import com.rossPalletScanApp.dto.BoxDto;
import com.rossPalletScanApp.dto.OcrLabelDto;
import com.rossPalletScanApp.entity.Box;
import com.rossPalletScanApp.entity.BoxContainerPool;
import com.rossPalletScanApp.entity.Pallet;
import com.rossPalletScanApp.repository.BoxContainerPoolRepository;
import com.rossPalletScanApp.repository.BoxRepository;
import com.rossPalletScanApp.repository.PalletRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AssignmentService {

	private final PalletRepository palletRepository;
	private final BoxRepository boxRepository;
	private final BoxContainerPoolRepository poolRepository;

	public AssignmentService(PalletRepository palletRepository, BoxRepository boxRepository,
			BoxContainerPoolRepository poolRepository) {
		this.palletRepository = palletRepository;
		this.boxRepository = boxRepository;
		this.poolRepository = poolRepository;
	}

	/**
	 * 3rd API: Assign box to pallet Returns: ResponseEntity with { status, message,
	 * data: BoxDto }
	 */
	@Transactional
	public ResponseEntity<Map<String, Object>> assignBoxToPallet(AssignBoxRequest request) {
		try {
			Pallet pallet = palletRepository.findById(request.palletId)
					.orElseThrow(() -> new EntityNotFoundException("Pallet not found"));

			List<Box> existing = pallet.getBoxes();
			int capacity = pallet.getCapacity();
			if (existing.size() >= capacity) {
				return bad("Pallet " + pallet.getCode() + " is full (" + capacity + ").");
			}

			OcrLabelDto o = request.box;
			String reqPo = safe(o.rossPo);
			String reqColor = safe(o.color);
			String reqSku = safe(o.rossSkuNumber);

			// Enforce pallet homogeneity
			if (!existing.isEmpty()) {
				Box first = existing.get(0);
				boolean sameCombo = eq(reqPo, first.getRossPo()) && eq(reqColor, first.getColor())
						&& eq(reqSku, first.getRossSkuNumber());
				if (!sameCombo) {
					return bad("Pallet " + pallet.getCode() + " has different combination.");
				}
			}

			// Get next container id from pool
			BoxContainerPool chosen = poolRepository.findFirstByAssignedFalseOrderByIdAsc()
					.orElseThrow(() -> new IllegalStateException("No available box containerId in pool"));
			chosen.setAssigned(true);
			poolRepository.save(chosen);

			// ===== Appointment number per PO =====
			String appointmentOrder;
			if (boxRepository.existsByRossPoIgnoreCase(reqPo)) {
				// Reuse existing appointment number for this PO
				appointmentOrder = boxRepository.findTopByRossPoIgnoreCaseOrderByIdAsc(reqPo)
						.map(Box::getAppointmentOrder)
						.orElseThrow(() -> new IllegalStateException("PO exists but no appointment order found"));
			} else {
				// New PO → allocate next global unique number
				Integer maxOrder = boxRepository.findMaxAppointmentOrder();
				int nextOrder = (maxOrder == null) ? 1 : maxOrder + 1;
				appointmentOrder = String.format("%03d", nextOrder);
			}

			// Build and save Box
			Box box = new Box();
			box.setPallet(pallet);
			box.setContainerId(chosen.getContainerId());

			// Copy OCR fields
			box.setImageFilename(o.imageFilename);
			box.setOcrConfidence(o.ocrConfidence);
			box.setRossPo(reqPo);
			box.setRossStyle(o.rossStyle);
			box.setItemDescription(o.itemDescription);
			box.setColor(reqColor);
			box.setRossSkuNumber(reqSku);
			box.setQuantity(o.quantity);
			box.setNetWeightKg(o.netWeightKg);
			box.setGrossWeightKg(o.grossWeightKg);
			box.setMeasurement(o.measurement);
			box.setConsignedTo(o.consignedTo);
			box.setDeliverTo(o.deliverTo);
			box.setDeliverToAddress(o.deliverToAddress);
			box.setCountryOfOrigin(o.countryOfOrigin);
			box.setCartonNo(o.cartonNo);
			box.setAppointmentOrder(appointmentOrder);

			Box saved = boxRepository.save(box);
			BoxDto dto = toDto(saved);

			Map<String, Object> body = new LinkedHashMap<>();
			body.put("status", "SUCCESS");
			body.put("message", "Box assigned to pallet successfully");
			body.put("data", dto);

			return ResponseEntity.ok(body);

		} catch (EntityNotFoundException | IllegalStateException | IllegalArgumentException ex) {
			return bad(ex.getMessage());
		}
	}

	/**
	 * Pallet Detail API: List boxes by pallet Returns: ResponseEntity with {
	 * status, message, count, data: List<BoxDto> }
	 */
	@Transactional(readOnly = true)
	public ResponseEntity<Map<String, Object>> listBoxesByPallet(Long palletId) {
		try {
			Pallet pallet = palletRepository.findById(palletId)
					.orElseThrow(() -> new EntityNotFoundException("Pallet not found"));

			List<BoxDto> list = boxRepository.findByPallet(pallet).stream().map(this::toDto).toList();

			Map<String, Object> body = new LinkedHashMap<>();
			body.put("status", "SUCCESS");
			body.put("message", "Boxes fetched");
			body.put("count", list.size());
			body.put("masterContainerId", pallet.getMasterContainerId());
			body.put("data", list);

			return ResponseEntity.ok(body);

		} catch (EntityNotFoundException ex) {
			return bad(ex.getMessage());
		}
	}

	// Lists all boxes grouped by pallet.
	@Transactional(readOnly = true)
	public ResponseEntity<Map<String, Object>> listAllBoxesGroupedByPallet() {
		List<Pallet> pallets = palletRepository.findAllByOrderByIdAsc();

		List<Map<String, Object>> groups = new ArrayList<>();

		for (Pallet p : pallets) {
			// Fetch boxes by palletId to avoid lazy proxy issues
			List<Box> boxes = boxRepository.findByPalletId(p.getId());

			// Map to DTOs
			List<BoxDto> boxDtos = boxes.stream().map(this::toDto).toList();

			// Derive combination from first box (if present)
			Map<String, String> combo = new LinkedHashMap<>();
			if (!boxes.isEmpty()) {
				Box first = boxes.get(0);
				combo.put("rossPo", safe(first.getRossPo()));
				combo.put("color", safe(first.getColor()));
				combo.put("rossSkuNumber", safe(first.getRossSkuNumber()));
			}

			Map<String, Object> group = new LinkedHashMap<>();
			group.put("palletId", p.getId());
			group.put("palletCode", p.getCode());
			group.put("masterContainerId", p.getMasterContainerId());
			group.put("capacity", p.getCapacity());
			group.put("boxCount", boxDtos.size());
			group.put("combination", combo);
			group.put("boxes", boxDtos);

			groups.add(group);
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", "SUCCESS");
		body.put("message", "Grouped boxes by pallet fetched");
		body.put("totalPallets", pallets.size());
		body.put("data", groups);

		return ResponseEntity.ok(body);
	}

	/**
	 * DELETE API: Delete a box and free its containerId back to the pool. Keeps
	 * pallets and master container IDs intact. Response: { status, message, boxId,
	 * containerId }
	 */
	@Transactional
	public ResponseEntity<Map<String, Object>> deleteBox(Long boxId) {
		Optional<Box> maybeBox = boxRepository.findById(boxId);
		if (maybeBox.isEmpty()) {
			return bad("Box not found: " + boxId);
		}

		Box box = maybeBox.get();
		String containerId = box.getContainerId();

		// Delete the box first
		boxRepository.delete(box);

		// Mark the containerId as unassigned (available again)
		int updated = 0;
		if (containerId != null && !containerId.isBlank()) {
			updated = poolRepository.unassignByContainerId(containerId);
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", "SUCCESS");
		body.put("message", "Box deleted and containerId returned to pool");
		body.put("boxId", boxId);
		body.put("containerId", containerId);
		body.put("containerReturned", updated == 1);

		return ResponseEntity.ok(body);
	}

	/**
	 * DELETE (bulk): Delete all boxes for a pallet and return their containerIds to
	 * the pool (assigned=false). Keeps pallet/master containerId intact. Response:
	 * { status, message, palletId, deletedCount, containerIdsFreed: [...],
	 * failures: [...] }
	 */
	@Transactional
	public ResponseEntity<Map<String, Object>> deleteBoxesByPallet(Long palletId) {
		// Verify pallet exists (avoid LazyInitialization by using exists)
		if (!palletRepository.existsById(palletId)) {
			return bad("Pallet not found: " + palletId);
		}

		// Fetch all boxes assigned to this pallet
		List<Box> boxes = boxRepository.findByPalletId(palletId);
		if (boxes.isEmpty()) {
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("status", "SUCCESS");
			body.put("message", "No boxes found for pallet; nothing to delete");
			body.put("palletId", palletId);
			body.put("deletedCount", 0);
			body.put("containerIdsFreed", List.of());
			return ResponseEntity.ok(body);
		}

		// Free containerIds in the pool
		List<String> freed = new ArrayList<>();
		List<String> failed = new ArrayList<>();
		for (Box b : boxes) {
			String cid = b.getContainerId();
			if (cid != null && !cid.isBlank()) {
				int updated = poolRepository.unassignByContainerId(cid);
				if (updated == 1) {
					freed.add(cid);
				} else {
					failed.add(cid); // pool row missing or already unassigned
				}
			}
		}

		// Delete boxes
		int deletedCount = boxRepository.deleteByPalletId(palletId);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", "SUCCESS");
		body.put("message", "Boxes deleted for pallet and containerIds returned to pool");
		body.put("palletId", palletId);
		body.put("deletedCount", deletedCount);
		body.put("containerIdsFreed", freed);
		if (!failed.isEmpty()) {
			body.put("containerIdsNotFoundOrAlreadyFreed", failed);
		}

		return ResponseEntity.ok(body);
	}

	// --------------------------
	// Helpers
	// --------------------------

	private ResponseEntity<Map<String, Object>> bad(String message) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", "FAILURE");
		body.put("message", message);
		return ResponseEntity.badRequest().body(body);
	}

	private BoxDto toDto(Box b) {
		BoxDto d = new BoxDto();
		d.id = b.getId();
		d.containerId = b.getContainerId();
		d.palletId = (b.getPallet() != null) ? b.getPallet().getId() : null;
		d.palletCode = (b.getPallet() != null) ? b.getPallet().getCode() : null;

		d.imageFilename = b.getImageFilename();
		d.ocrConfidence = b.getOcrConfidence();
		d.rossPo = b.getRossPo();
		d.rossStyle = b.getRossStyle();
		d.itemDescription = b.getItemDescription();
		d.color = b.getColor();
		d.rossSkuNumber = b.getRossSkuNumber();
		d.quantity = b.getQuantity();
		d.netWeightKg = b.getNetWeightKg();
		d.grossWeightKg = b.getGrossWeightKg();
		d.measurement = b.getMeasurement();
		d.consignedTo = b.getConsignedTo();
		d.deliverTo = b.getDeliverTo();
		d.deliverToAddress = b.getDeliverToAddress();
		d.countryOfOrigin = b.getCountryOfOrigin();
		d.cartonNo = b.getCartonNo();
		d.appointmentOrder = b.getAppointmentOrder();
		d.createdAt = b.getCreatedAt();
		return d;
	}

	private String safe(String s) {
		return (s == null) ? "" : s.trim();
	}

	private boolean eq(String a, String b) {
		return safe(a).equalsIgnoreCase(safe(b));
	}
}