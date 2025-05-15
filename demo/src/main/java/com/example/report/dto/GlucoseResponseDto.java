package com.example.report.dto;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record GlucoseResponseDto(
	Integer value,
	String displayApp,
	String trend,
	LocalDateTime recordedAt
) {
}

