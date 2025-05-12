package com.example.demo.dto;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record CGMDto(
	String isConnected,
	Integer maxGlucose,
	Integer minGlucose,
	LocalDateTime lastEgvTime,
	String accessToken,
	String refreshToken,
	LocalDateTime issuedAt,
	LocalDateTime expiresIn
) {
}
