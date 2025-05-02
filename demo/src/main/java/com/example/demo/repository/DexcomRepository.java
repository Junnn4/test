package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.demo.entity.Dexcom;

public interface DexcomRepository extends JpaRepository <Dexcom, Long> {

	Optional<Dexcom> findByUserId(Long userId);

	@Query("SELECT d FROM Dexcom d WHERE d.isConnected = 'connected'")
	List<Dexcom> findAllConnectedUsers();

}
