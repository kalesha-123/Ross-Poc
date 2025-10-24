package com.rossPalletScanApp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rossPalletScanApp.dto.OcrLabelDto;
import com.rossPalletScanApp.dto.PalletAvailabilityDto;
import com.rossPalletScanApp.entity.Box;
import com.rossPalletScanApp.entity.Pallet;
import com.rossPalletScanApp.repository.PalletRepository;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class PalletService {

 private final PalletRepository palletRepository;

 public PalletService(PalletRepository palletRepository) {
     this.palletRepository = palletRepository;
 }

 public List<PalletAvailabilityDto> checkAvailability(OcrLabelDto ocr) {
     String reqPo = safe(ocr.rossPo);
     String reqColor = safe(ocr.color);
     String reqSku = safe(ocr.rossSkuNumber);

     List<Pallet> pallets = palletRepository.findAllByOrderByIdAsc();

     boolean anyHasRequestedCombination = pallets.stream().anyMatch(p -> {
         List<Box> boxes = p.getBoxes();
         if (boxes.isEmpty()) return false;
         // Check if pallet contains at least one box with requested combination
         return boxes.stream().anyMatch(b ->
                 eq(reqPo, b.getRossPo()) &&
                 eq(reqColor, b.getColor()) &&
                 eq(reqSku, b.getRossSkuNumber())
         );
     });

     List<PalletAvailabilityDto> result = new ArrayList<>();

     for (Pallet p : pallets) {
         List<Box> boxes = p.getBoxes();
         int count = boxes.size();

         boolean canAccept;
         String reason;

         if (count == 0) {
             if (anyHasRequestedCombination) {
                 // Prefer filling existing pallets with same combo; empty pallets are false
                 canAccept = false;
                 reason = "Empty pallet; other pallet(s) already carry this combination. Prefer filling those.";
             } else {
                 // No pallet has requested combo yet → empty pallets are true
                 canAccept = true;
                 reason = "Empty pallet; can start new combination here.";
             }
         } else {
             // Determine pallet's current combination from first box (homogeneous expected)
             Box first = boxes.get(0);
             boolean sameCombo = eq(reqPo, first.getRossPo()) &&
                                 eq(reqColor, first.getColor()) &&
                                 eq(reqSku, first.getRossSkuNumber());

             if (sameCombo) {
                 if (count < p.getCapacity()) {
                     canAccept = true;
                     reason = "Same combination and has space.";
                 } else {
                     canAccept = false;
                     reason = "Same combination but pallet is full.";
                 }
             } else {
                 canAccept = false;
                 reason = "Different combination present.";
             }
         }

         result.add(new PalletAvailabilityDto(
                 p.getId(), p.getCode(), p.getMasterContainerId(),
                 p.getCapacity(), count, canAccept, reason
         ));
     }

     return result;
 }

 private String safe(String s) {
     return (s == null) ? "" : s.trim();
 }
 private boolean eq(String a, String b) {
     return safe(a).equalsIgnoreCase(safe(b));
 }
}