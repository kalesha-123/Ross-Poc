package com.rossPalletScanApp.entity;


import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "boxes")
public class Box {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 // Allocated from pool
 @Column(nullable = false, unique = true)
 private String containerId;

 @ManyToOne(fetch = FetchType.LAZY)
 @JoinColumn(name = "pallet_id")
 private Pallet pallet;

 // From OCR (1st API)
 private String imageFilename;
 private Integer ocrConfidence;
 private String rossPo;
 private String rossStyle;
 private String itemDescription;
 private String color;
 private String rossSkuNumber;
 private String quantity;
 private String netWeightKg;
 private String grossWeightKg;
 private String measurement;
 private String consignedTo;
 private String deliverTo;
 private String deliverToAddress;
 private String countryOfOrigin;
 private String cartonNo;
 @Column(length = 3)
 private String appointmentOrder;

 public String getAppointmentOrder() {
	return appointmentOrder;
}

public void setAppointmentOrder(String appointmentOrder) {
	this.appointmentOrder = appointmentOrder;
}
@Column(nullable = false)
 private OffsetDateTime createdAt = OffsetDateTime.now();

 public Box() {}

 // getters/setters

 public Long getId() { return id; }
 public String getContainerId() { return containerId; }
 public void setContainerId(String containerId) { this.containerId = containerId; }

 public Pallet getPallet() { return pallet; }
 public void setPallet(Pallet pallet) { this.pallet = pallet; }

 public String getImageFilename() { return imageFilename; }
 public void setImageFilename(String imageFilename) { this.imageFilename = imageFilename; }

 public Integer getOcrConfidence() { return ocrConfidence; }
 public void setOcrConfidence(Integer ocrConfidence) { this.ocrConfidence = ocrConfidence; }

 public String getRossPo() { return rossPo; }
 public void setRossPo(String rossPo) { this.rossPo = rossPo; }

 public String getRossStyle() { return rossStyle; }
 public void setRossStyle(String rossStyle) { this.rossStyle = rossStyle; }

 public String getItemDescription() { return itemDescription; }
 public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }

 public String getColor() { return color; }
 public void setColor(String color) { this.color = color; }

 public String getRossSkuNumber() { return rossSkuNumber; }
 public void setRossSkuNumber(String rossSkuNumber) { this.rossSkuNumber = rossSkuNumber; }

 public String getQuantity() { return quantity; }
 public void setQuantity(String quantity) { this.quantity = quantity; }

 public String getNetWeightKg() { return netWeightKg; }
 public void setNetWeightKg(String netWeightKg) { this.netWeightKg = netWeightKg; }

 public String getGrossWeightKg() { return grossWeightKg; }
 public void setGrossWeightKg(String grossWeightKg) { this.grossWeightKg = grossWeightKg; }

 public String getMeasurement() { return measurement; }
 public void setMeasurement(String measurement) { this.measurement = measurement; }

 public String getConsignedTo() { return consignedTo; }
 public void setConsignedTo(String consignedTo) { this.consignedTo = consignedTo; }

 public String getDeliverTo() { return deliverTo; }
 public void setDeliverTo(String deliverTo) { this.deliverTo = deliverTo; }

 public String getDeliverToAddress() { return deliverToAddress; }
 public void setDeliverToAddress(String deliverToAddress) { this.deliverToAddress = deliverToAddress; }

 public String getCountryOfOrigin() { return countryOfOrigin; }
 public void setCountryOfOrigin(String countryOfOrigin) { this.countryOfOrigin = countryOfOrigin; }

 public String getCartonNo() { return cartonNo; }
 public void setCartonNo(String cartonNo) { this.cartonNo = cartonNo; }

 public OffsetDateTime getCreatedAt() { return createdAt; }
 public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}