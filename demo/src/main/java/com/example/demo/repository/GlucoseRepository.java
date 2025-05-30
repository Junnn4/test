package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.entity.Glucose;

public interface GlucoseRepository extends JpaRepository<Glucose, Long> {

	// cgm.dexcomId로 매핑
	List<Glucose> findByCgm_DexcomIdAndRecordedAtBetween(Long dexcomId, LocalDateTime start, LocalDateTime end);

	List<Glucose> findByCgm_DexcomId(Long dexcomId);

	@Query("SELECT g.recordedAt FROM Glucose g WHERE g.cgm.dexcomId = :dexcomId AND g.recordedAt BETWEEN :start AND :end")
	List<LocalDateTime> findTimesByDexcomIdAndTimeRange(
		@Param("dexcomId") Long dexcomId,
		@Param("start") LocalDateTime start,
		@Param("end") LocalDateTime end
	);
}
