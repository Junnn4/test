package com.example.demo.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.example.demo.common.DexcomConfig;
import com.example.demo.convert.DexcomConverter;
import com.example.demo.dto.DexcomDto;
import com.example.demo.entity.Dexcom;
import com.example.demo.repository.DexcomRepository;
import com.example.demo.service.DexcomService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DexcomService {
	private final DexcomRepository dexcomRepository;
	private final DexcomAuthService dexcomAuthService;
	private final DexcomConfig dexcomConfig;

	private final RestTemplate restTemplate = new RestTemplate();

	@Transactional
	public void saveDexcomSettingInfo(Long userId, String responseBody) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(responseBody);
			JsonNode records = root.get("records");

			if (!records.isArray() || records.isEmpty()) {
				throw new RuntimeException("기기 정보 없음");
			}

			JsonNode device = records.get(0);

			String lastUpload = device.get("lastUploadDate").asText();
			OffsetDateTime uploadTime = OffsetDateTime.parse(lastUpload);
			OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

			String isConnected = uploadTime.plusHours(6).isBefore(now) ? "disconnected" : "connected";

			Integer maxGlucose = 250;
			Integer minGlucose = 70;

			JsonNode alertSchedules = device.get("alertSchedules");
			if (alertSchedules != null && alertSchedules.isArray() && alertSchedules.size() > 0) {
				JsonNode alertSettings = alertSchedules.get(0).get("alertSettings");
				if (alertSettings != null && alertSettings.isArray()) {
					for (JsonNode alert : alertSettings) {
						String alertName = alert.get("alertName").asText();
						int value = alert.get("value").asInt();

						if ("high".equals(alertName)) {
							maxGlucose = value;
						} else if ("low".equals(alertName)) {
							minGlucose = value;
						}
					}
				}
			}

			LocalDateTime lastEgvTime = uploadTime.toLocalDateTime();  // OffsetDateTime → LocalDateTime 변환

			Dexcom dexcom = DexcomConverter.createInfo(userId, isConnected, maxGlucose, minGlucose, lastEgvTime);
			dexcomRepository.save(dexcom);

			log.info("Dexcom 설정 정보 저장 완료: 상태={}, max={}, min={}, time={}", isConnected, maxGlucose, minGlucose, lastEgvTime);

		} catch (Exception e) {
			log.error("Dexcom 설정 정보 파싱 실패", e);
			throw new RuntimeException("Dexcom 설정 정보 저장 실패", e);
		}
	}

	@Transactional
	public String updateDeviceInfo(Long userId) {
		Dexcom dexcom = dexcomRepository.findByUserId(userId)
			.orElseThrow(() -> new IllegalArgumentException("해당 유저의 Dexcom 정보가 없습니다."));

		String accessToken = dexcomAuthService.getAccessTokenByUserId(userId);

		if (accessToken == null) {
			return "access_token 없음. 먼저 인증하세요.";
		}

		String url = dexcomConfig.getDEVICE_ENDPOINT();

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<Void> request = new HttpEntity<>(headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(
				url,
				HttpMethod.GET,
				request,
				String.class
			);

			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode root = objectMapper.readTree(response.getBody());

			JsonNode records = root.path("records");
			if (records.isArray() && records.size() > 0) {
				JsonNode firstRecord = records.get(0);
				String lastUploadDateStr = firstRecord.path("lastUploadDate").asText();

				if (!lastUploadDateStr.isEmpty()) {
					OffsetDateTime uploadTime = OffsetDateTime.parse(lastUploadDateStr);
					OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

					if (uploadTime.plusHours(6).isBefore(now)) {
						dexcom.setIsConnected("disconnected");
						dexcomRepository.save(dexcom);
						return "덱스콤에 연결 되어있지않습니다.";
					} else {
						dexcom.setIsConnected("connected");
						dexcomRepository.save(dexcom);
						return "덱스콤에 연결 되어있습니다.";
					}
				}
			}
			log.info("Dexcom raw body: {}", response.getBody());
			log.info("Dexcom HTTP Status code: {}", response.getStatusCode());

			dexcom.setIsConnected("unknown");
			dexcomRepository.save(dexcom);
			return "기기 정보가 충분하지 않습니다.";
		} catch (Exception e) {
			log.error("Dexcom 기기 정보 업데이트 중 오류 발생", e);
			return "에러가 발생했습니다.";
		}
	}

	@Transactional
	public String updateMinMaxGlucose(Long userId, Integer min, Integer max) {
		Dexcom dexcom = dexcomRepository.findByUserId(userId)
			.orElseThrow(() -> new IllegalArgumentException("해당 유저의 Dexcom 정보가 없습니다."));

		boolean updated = false;

		if (min != null && min > 0) {
			dexcom.setMinGlucose(min);
			updated = true;
		}
		if (max != null && max > 0) {
			dexcom.setMaxGlucose(max);
			updated = true;
		}

		if (updated) {
			log.info("사용자 {}의 min_glucose={}, max_glucose={} 업데이트 완료", userId, min, max);
			return "success";
		} else {
			log.warn("사용자 {}의 설정 실패: min={}, max={}", userId, min, max);
			return "failed";
		}
	}

	public ResponseEntity<DexcomDto> getDexcomInfo(Long userId) {
		Dexcom dexcom = dexcomRepository.findByUserId(userId)
			.orElseThrow(() -> new IllegalArgumentException("해당 유저의 Dexcom 정보가 없습니다."));

		return ResponseEntity.ok().body(DexcomConverter.EntityToDto(dexcom));
	}
}
