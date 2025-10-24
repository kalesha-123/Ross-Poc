package com.rossPalletScanApp.dto;

import jakarta.validation.constraints.NotNull;

public class AssignBoxRequest {

	@NotNull
	public Long palletId;

	@NotNull
	public OcrLabelDto box; // full OCR payload from API 1

	// Optional: allow client to specify containerId (if you don’t want
	// auto-allocation)
//	public String preferredContainerId;
}