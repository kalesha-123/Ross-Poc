package com.rossPalletScanApp.repository;

import com.rossPalletScanApp.entity.BoxContainerPool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BoxContainerPoolRepository extends JpaRepository<BoxContainerPool, Long> {

	/**
	 * Returns exactly one unassigned container (the first by id) if present. This
	 * uses Spring Data's derived query with ORDER BY + FIRST to ensure only one
	 * row.
	 */
	Optional<BoxContainerPool> findFirstByAssignedFalseOrderByIdAsc();

	Optional<BoxContainerPool> findByContainerId(String containerId);

	// ✅ Mark a containerId as available again
	@Modifying
	@Query("UPDATE BoxContainerPool b SET b.assigned = false WHERE b.containerId = :containerId")
	int unassignByContainerId(String containerId);

}