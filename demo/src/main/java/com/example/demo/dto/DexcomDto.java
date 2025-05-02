package com.example.demo.dto;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record DexcomDto(
	String isConnected,
	Integer maxGlucose,
	Integer minGlucose,
	LocalDateTime lastEgvTime
) {
}
