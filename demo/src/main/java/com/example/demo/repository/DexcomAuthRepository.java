package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.entity.DexcomAuth;

public interface DexcomAuthRepository extends JpaRepository<DexcomAuth, Long> {

	Optional<DexcomAuth> findByDexcomId(Long dexcomId);

	@Modifying
	@Query("UPDATE DexcomAuth d SET d.accessToken = :accessToken, d.updatedAt = CURRENT_TIMESTAMP WHERE d.dexcomId = :dexcomId")
	void updateAccessToken(@Param("dexcomId") Long dexcomId, @Param("accessToken") String accessToken);


	Optional<DexcomAuth> findByDexcom_UserId(Long userId);
}
