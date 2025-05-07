package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.DexcomAuth;

public interface DexcomAuthRepository extends JpaRepository<DexcomAuth, Long> {

	Optional<DexcomAuth> findByDexcomId(Long dexcomId);

	Optional<DexcomAuth> findByDexcom_UserId(Long userId);
}
