package com.rossPalletScanApp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rossPalletScanApp.entity.Box;
import com.rossPalletScanApp.entity.Pallet;

import java.util.List;
import java.util.Optional;

public interface BoxRepository extends JpaRepository<Box, Long> {

	List<Box> findByPallet(Pallet pallet);

	List<Box> findByPalletId(Long palletId);

	// Highest numeric appointment order stored as a string "001","002",...
	@Query("select max(cast(b.appointmentOrder as integer)) from Box b")
	Integer findMaxAppointmentOrder();

	// Fast existence check (case-insensitive)
	boolean existsByRossPoIgnoreCase(String rossPo);

	// Deterministic single row for a PO (earliest box by id)
	Optional<Box> findTopByRossPoIgnoreCaseOrderByIdAsc(String rossPo);

	@Modifying
	@Query("delete from Box b where b.pallet.id = :palletId")
	int deleteByPalletId(@Param("palletId") Long palletId);
}
