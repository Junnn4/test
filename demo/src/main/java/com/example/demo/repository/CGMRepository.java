package com.example.demo.repository;

import java.util.Optional;

import com.example.demo.entity.CGM;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CGMRepository extends JpaRepository<CGM, Long> {

	Optional<CGM> findByUserId(Long userId);

	Optional<CGM> findByDexcomId(Long dexcomId);

	@Modifying
	@Query("UPDATE CGM c SET c.accessToken = :accessToken, c.refreshToken = :refreshToken, c.issuedAt = :issuedAt, c.expiresIn = :expiresIn WHERE c.dexcomId = :dexcomId")
	void updateTokens(Long dexcomId, String accessToken, String refreshToken, java.time.LocalDateTime issuedAt, java.time.LocalDateTime expiresIn);
}
