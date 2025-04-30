package com.example.demo.convert;

import java.time.LocalDateTime;

import com.example.demo.entity.Dexcom;
import com.example.demo.entity.DexcomAuth;
import com.example.demo.entity.User;

public class DexcomConverter {

	public static Dexcom createInfo(Long userId, String isConnected, Integer maxGlucose, Integer minGlucose, LocalDateTime lastEgvTime) {
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
}
