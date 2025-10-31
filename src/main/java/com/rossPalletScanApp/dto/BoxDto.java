package com.rossPalletScanApp.dto;

import java.time.OffsetDateTime;

public class BoxDto {
	public Long id;
	public String containerId;
	public Long palletId;
	public String palletCode;

	public String imageFilename;
	public Integer ocrConfidence;
	public String rossPo;
	public String rossStyle;
	public String itemDescription;
	public String color;
	public String rossSkuNumber;
	public String quantity;
	public String netWeightKg;
	public String grossWeightKg;
	public String measurement;
	public String consignedTo;
	public String deliverTo;
	public String deliverToAddress;
	public String countryOfOrigin;
	public String cartonNo;
	public String appointmentOrder;

	public OffsetDateTime createdAt;
}