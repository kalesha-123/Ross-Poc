package com.rossPalletScanApp.dto;

public class PalletAvailabilityDto {
 public Long palletId;
 public String palletCode;
 public String masterContainerId;
 public int capacity;
 public int currentBoxCount;
 public boolean canAccept;
 public String reason;

 public PalletAvailabilityDto() {}

 public PalletAvailabilityDto(Long palletId, String palletCode, String masterContainerId,
                              int capacity, int currentBoxCount, boolean canAccept, String reason) {
     this.palletId = palletId;
     this.palletCode = palletCode;
     this.masterContainerId = masterContainerId;
     this.capacity = capacity;
     this.currentBoxCount = currentBoxCount;
     this.canAccept = canAccept;
     this.reason = reason;
 }
}