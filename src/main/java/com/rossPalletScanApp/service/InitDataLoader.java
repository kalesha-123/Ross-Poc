package com.rossPalletScanApp.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.rossPalletScanApp.entity.BoxContainerPool;
import com.rossPalletScanApp.entity.Pallet;
import com.rossPalletScanApp.repository.BoxContainerPoolRepository;
import com.rossPalletScanApp.repository.PalletRepository;

import java.util.List;

@Component
public class InitDataLoader implements CommandLineRunner {

 private final PalletRepository palletRepository;
 private final BoxContainerPoolRepository poolRepository;

 public InitDataLoader(PalletRepository palletRepository,
                       BoxContainerPoolRepository poolRepository) {
     this.palletRepository = palletRepository;
     this.poolRepository = poolRepository;
 }

 @Override
 public void run(String... args) throws Exception {
     if (palletRepository.count() == 0) {
         String[] masters = new String[] {
             "03000150000002708806",
             "03000150000002708813",
             "03000150000002708967",
             "03000150000002783964",
             "03000150000002784064",
             "03000150000002784164",
             "03000150000002784264",
             "03000150000002784364",
             "03000150000002784464",
             "03000150000002784564"
         };
         for (int i = 0; i < masters.length; i++) {
             Pallet p = new Pallet("Pallet " + (i + 1), masters[i], 3);
             palletRepository.save(p);
         }
     }

     if (poolRepository.count() == 0) {
         List<String> boxIds = List.of(
             "02000150000030922817",
             "02000150000030922800",
             "02000150000030922701",
             "02000150000030922718",
             "02000150000030922725",
             "02000150000030922732",
             "02000150000030922749",
             "02000150000030922756",
             "02000150000030922763",
             "02000150000030922794",
             "02000150000030922787",
             "02000150000030922770",
             "02000150000030922879",
             "02000150000030922886",
             "02000150000030922893",
             "02000150000030922824",
             "02000150000030922848",
             "02000150000030922855",
             "02000150000030922862",
             "02000150000030922831",
             "02000150000030923555",
             "02000150000030923548",
             "02000150000030923531",
             "02000150000030923524",
             "02000150000030923517",
             "02000150000030923500",
             "02000150000030923630",
             "02000150000030923623",
             "02000150000030923616",
             "02000150000030923609",
             "02000150000030923593"
         );
         boxIds.forEach(id -> poolRepository.save(new BoxContainerPool(id)));
     }
 }
}