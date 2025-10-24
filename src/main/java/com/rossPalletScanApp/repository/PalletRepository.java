package com.rossPalletScanApp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rossPalletScanApp.entity.Pallet;

import java.util.List;

public interface PalletRepository extends JpaRepository<Pallet, Long> {
 List<Pallet> findAllByOrderByIdAsc();
}