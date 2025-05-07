package com.example.demo.convert;

import java.time.LocalDateTime;

import com.example.demo.dto.DexcomDto;
import com.example.demo.entity.Dexcom;
import com.example.demo.entity.DexcomAuth;

public class DexcomConverter {
	public static Dexcom createInfo(Long userId, String isConnected, int maxGlucose, int minGlucose, LocalDateTime lastEgvTime) {
		return Dexcom.builder()
			.userId(userId)
			.isConnected(isConnected)
			.maxGlucose(maxGlucose)
			.minGlucose(minGlucose)
			.lastEgvTime(lastEgvTime)
			.build();
	}

	public static DexcomAuth create(Long dexcomId, String accessToken, String refreshToken, LocalDateTime updatedAt, LocalDateTime expiresAt) {
		return DexcomAuth.builder()
			.dexcomId(dexcomId)
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.updatedAt(updatedAt)
			.expiresIn(expiresAt)
			.build();
	}

	public static DexcomDto EntityToDto(Dexcom dexcom) {
		return DexcomDto.builder()
			.isConnected(dexcom.getIsConnected())
			.minGlucose(dexcom.getMinGlucose())
			.maxGlucose(dexcom.getMaxGlucose())
			.lastEgvTime(dexcom.getLastEgvTime())
			.build();
	}
}
