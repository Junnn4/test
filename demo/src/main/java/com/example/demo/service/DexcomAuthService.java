package com.example.demo.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.demo.convert.DexcomConverter;
import com.example.demo.entity.DexcomAuth;
import com.example.demo.repository.DexcomAuthRepository;
import com.example.demo.repository.DexcomRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DexcomAuthService {

	private final DexcomAuthRepository dexcomAuthRepository;
	private final DexcomRepository dexcomRepository;

	public void saveToken(Long userId, String accessToken, String refreshToken) {

		Long dexcomId = dexcomRepository.findByUserId(userId)
			.orElseThrow(() -> new RuntimeException("Dexcom 정보 없음"))
			.getDexcomId();

		DexcomAuth dexcomAuth = DexcomConverter.create(dexcomId, accessToken, refreshToken, LocalDateTime.now(), LocalDateTime.now().plusHours(2));
		dexcomAuthRepository.save(dexcomAuth);
	}

	public String getRefreshTokenByUserId(Long userId) {

		Long dexcomId = dexcomRepository.findByUserId(userId)
			.orElseThrow(() -> new RuntimeException("Dexcom 정보 없음"))
			.getDexcomId();

		String refreshToken = dexcomAuthRepository.findById(dexcomId)
			.orElseThrow(() -> new RuntimeException("Dexcom Auth 정보 없음"))
			.getRefreshToken();

		return refreshToken;
	}

	public String getAccessTokenByUserId(Long userId) {
		Long dexcomId = dexcomRepository.findByUserId(userId)
			.orElseThrow(() -> new RuntimeException("Dexcom 정보 없음"))
			.getDexcomId();

		String accessToken = dexcomAuthRepository.findById(dexcomId)
			.orElseThrow(() -> new RuntimeException("Dexcom Auth 정보 없음"))
			.getAccessToken();

		return accessToken;
	}

	public void updateAccessTokenByRefreshToken(Long userId, String accessToken) {
		Long dexcomId = dexcomRepository.findByUserId(userId)
			.orElseThrow(() -> new RuntimeException("Dexcom 정보 없음"))
			.getDexcomId();

		DexcomAuth dexcomAuth = dexcomAuthRepository.findById(dexcomId)
			.orElseThrow(() -> new RuntimeException("Dexcom Auth 정보 없음"));

		dexcomAuth.updateAccessToken(accessToken);
			dexcomAuthRepository.save(dexcomAuth);
	}
}
