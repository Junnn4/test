package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.entity.DexcomAuth;

public interface DexcomAuthRepository extends JpaRepository<DexcomAuth, Long> {

	Optional<DexcomAuth> findByDexcomId(Long dexcomId);

	@Modifying
	@Query("UPDATE DexcomAuth da SET da.accessToken = :accessToken, da.refreshToken = :refreshToken, da.issuedAt = :issuedAt, da.expiresIn = :expiresIn WHERE da.dexcomId = :dexcomId")
	void updateTokens(@Param("dexcomId") Long dexcomId,
		@Param("accessToken") String accessToken,
		@Param("refreshToken") String refreshToken,
		@Param("issuedAt") LocalDateTime issuedAt,
		@Param("expiresIn") LocalDateTime expiresIn);
}
