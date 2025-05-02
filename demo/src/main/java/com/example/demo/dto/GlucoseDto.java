package com.example.demo.dto;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record GlucoseDto(
	Integer value,
	String displayApp,
	String trend,
	LocalDateTime recordedAt
) {
}
