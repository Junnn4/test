package com.example.demo.convert;

import java.time.LocalDateTime;

import com.example.demo.dto.CGMDto;
import com.example.demo.entity.CGM;

public class CGMConverter {
	public static CGM createInfo(Long userId, String isConnected, int maxGlucose, int minGlucose, LocalDateTime lastEgvTime, String accessToken, String refreshToken, LocalDateTime issuedAt, LocalDateTime expiresAt) {
		return CGM.builder()
			.userId(userId)
			.isConnected(isConnected)
			.maxGlucose(maxGlucose)
			.minGlucose(minGlucose)
			.lastEgvTime(lastEgvTime)
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.issuedAt(issuedAt)
			.expiresIn(expiresAt)
			.build();
	}


	public static CGMDto EntityToDto(CGM cgm) {
		return CGMDto.builder()
			.isConnected(cgm.getIsConnected())
			.minGlucose(cgm.getMinGlucose())
			.maxGlucose(cgm.getMaxGlucose())
			.lastEgvTime(cgm.getLastEgvTime())
			.accessToken(cgm.getAccessToken())
			.refreshToken(cgm.getRefreshToken())
			.issuedAt(cgm.getIssuedAt())
			.expiresIn(cgm.getExpiresIn())
			.build();
	}
}
