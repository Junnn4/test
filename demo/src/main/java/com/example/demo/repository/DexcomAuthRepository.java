package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.DexcomAuth;

public interface DexcomAuthRepository extends JpaRepository<DexcomAuth, Long> {
}
