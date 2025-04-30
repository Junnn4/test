package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Dexcom;

public interface DexcomRepository extends JpaRepository <Dexcom, Long> {

	Optional<Dexcom> findByUserId(Long userId);
}
