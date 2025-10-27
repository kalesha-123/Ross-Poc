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
        String reqPo   = safe(ocr.rossPo);
        String reqColor= safe(ocr.color);
        String reqSku  = safe(ocr.rossSkuNumber);

        List<Pallet> pallets = palletRepository.findAllByOrderByIdAsc();

        // Does any pallet already carry this combination (homogeneous)?
        boolean anySameComboExists = pallets.stream().anyMatch(
            p -> isSameComboPallet(p, reqPo, reqColor, reqSku)
        );

        // Among pallets with the same combo, do any have free capacity?
        boolean anySameComboHasSpace = pallets.stream().anyMatch(
            p -> isSameComboPallet(p, reqPo, reqColor, reqSku) &&
                 p.getBoxes().size() < p.getCapacity()
        );

        List<PalletAvailabilityDto> result = new ArrayList<>();

        for (Pallet p : pallets) {
            List<Box> boxes = p.getBoxes();
            int count = boxes.size();

            boolean canAccept;
            String reason;

            if (count == 0) {
                if (anySameComboHasSpace) {
                    // There is a pallet with the same combo and space → prefer filling it
                    canAccept = false;
                    reason = "Empty pallet; another pallet with same combination has space. Prefer filling that.";
                } else {
                    // Either no same-combo pallets exist or all of them are full → start new pallet
                    canAccept = true;
                    reason = anySameComboExists
                            ? "All pallets with same combination are full; can start a new pallet for this combination."
                            : "No pallet has this combination yet; can start new combination here.";
                }
            } else {
                boolean sameComboThisPallet = isSameComboPallet(p, reqPo, reqColor, reqSku);

                if (sameComboThisPallet) {
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

    // A pallet is considered "same combination" only if it's non-empty and all boxes match.
    private boolean isSameComboPallet(Pallet p, String reqPo, String reqColor, String reqSku) {
        List<Box> boxes = p.getBoxes();
        if (boxes == null || boxes.isEmpty()) return false;

        return boxes.stream().allMatch(b ->
            eq(reqPo, b.getRossPo()) &&
            eq(reqColor, b.getColor()) &&
            eq(reqSku, b.getRossSkuNumber())
        );
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    private boolean eq(String a, String b) {
        return safe(a).equalsIgnoreCase(safe(b));
    }
}
