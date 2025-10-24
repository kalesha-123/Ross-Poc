package com.rossPalletScanApp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.rossPalletScanApp.entity.Box;
import com.rossPalletScanApp.entity.Pallet;

import java.util.List;

public interface BoxRepository extends JpaRepository<Box, Long> {
	List<Box> findByPallet(Pallet pallet);

	List<Box> findByPalletId(Long palletId);

	@Modifying
	@Query("DELETE FROM Box b WHERE b.pallet.id = :palletId")
	int deleteByPalletId(Long palletId);

}