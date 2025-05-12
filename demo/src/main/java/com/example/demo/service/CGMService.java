package com.example.demo.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import com.example.demo.common.DexcomConfig;
import com.example.demo.common.error.GlobalErrorCodes;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.convert.CGMConverter;
import com.example.demo.dto.CGMDto;
import com.example.demo.entity.CGM;
import com.example.demo.repository.CGMRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CGMService {

	private final CGMRepository cgmRepository;
	private final DexcomConfig dexcomConfig;
	private final RestTemplate restTemplate = new RestTemplate();

	@Transactional
	public String exchangeCodeToToken(Map<String, String> params, Long userId) {
		String code = params.get("code");

		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("client_id", dexcomConfig.getClientId());
		body.add("client_secret", dexcomConfig.getClientSecret());
		body.add("code", code);
		body.add("grant_type", "authorization_code");
		body.add("redirect_uri", dexcomConfig.getRedirectUri());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		ResponseEntity<Map> resp = restTemplate.postForEntity(
			dexcomConfig.getTOKEN_ENDPOINT(),
			new HttpEntity<>(body, headers),
			Map.class
		);

		if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
			log.error("exchangeCodeToToken: 실패 status={}", resp.getStatusCode());
			throw new BusinessException(GlobalErrorCodes.DEXCOM_TOKEN_REQUEST_FAILED);
		}

		String accessToken = (String) resp.getBody().get("access_token");
		String refreshToken = (String) resp.getBody().get("refresh_token");

		String deviceBody = getDeviceInfo(accessToken);
		LocalDateTime now = LocalDateTime.now();

		CGM cgm = cgmRepository.findByUserId(userId).orElseGet(() ->
			CGMConverter.createInfo(userId, "unknown", 250, 70, now, accessToken, refreshToken, now, now.plusHours(2))
		);

		saveDexcomSettingInfo(cgm, deviceBody);
		cgm.setAccessToken(accessToken);
		cgm.setRefreshToken(refreshToken);
		cgm.setIssuedAt(now);
		cgm.setExpiresIn(now.plusHours(2));

		cgmRepository.save(cgm);
		return "Successfully issued token";
	}

	@Transactional
	public String refreshAccessToken(Long dexcomId) {
		CGM cgm = cgmRepository.findByDexcomId(dexcomId)
			.orElseThrow(() -> new BusinessException(GlobalErrorCodes.DEXCOM_NOT_FOUND));

		if (cgm.getRefreshToken() == null) {
			throw new BusinessException(GlobalErrorCodes.DEXCOM_NO_REFRESH_TOKEN);
		}

		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("client_id", dexcomConfig.getClientId());
		body.add("client_secret", dexcomConfig.getClientSecret());
		body.add("grant_type", "refresh_token");
		body.add("refresh_token", cgm.getRefreshToken());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		ResponseEntity<Map> resp = restTemplate.postForEntity(
			dexcomConfig.getTOKEN_ENDPOINT(),
			new HttpEntity<>(body, headers),
			Map.class
		);

		if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
			throw new BusinessException(GlobalErrorCodes.DEXCOM_TOKEN_REFRESH_FAILED);
		}

		String newAccessToken = (String) resp.getBody().get("access_token");
		String newRefreshToken = (String) resp.getBody().get("refresh_token");
		LocalDateTime issuedAt = LocalDateTime.now();
		LocalDateTime expiresAt = issuedAt.plusHours(2);

		cgmRepository.updateTokens(dexcomId, newAccessToken, newRefreshToken, issuedAt, expiresAt);
		return "Token refreshed successfully";
	}

	@Transactional
	public String updateDeviceInfo(Long dexcomId) {
		CGM cgm = cgmRepository.findByDexcomId(dexcomId)
			.orElseThrow(() -> new BusinessException(GlobalErrorCodes.DEXCOM_NOT_FOUND));

		if (cgm.getExpiresIn() == null || cgm.getExpiresIn().isBefore(LocalDateTime.now())) {
			refreshAccessToken(dexcomId);
		}

		String deviceBody = getDeviceInfo(cgm.getAccessToken());
		saveDexcomSettingInfo(cgm, deviceBody);
		cgmRepository.save(cgm);

		return cgm.getIsConnected().equals("connected")
			? "연결되어 있습니다"
			: "연결되어 있지 않습니다";
	}

	private String getDeviceInfo(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		headers.setContentType(MediaType.APPLICATION_JSON);

		ResponseEntity<String> response = restTemplate.exchange(
			dexcomConfig.getDEVICE_ENDPOINT(),
			HttpMethod.GET,
			new HttpEntity<>(headers),
			String.class
		);

		if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
			throw new BusinessException(GlobalErrorCodes.DEXCOM_DEVICE_INFO_FETCH_FAILED);
		}
		return response.getBody();
	}

	private void saveDexcomSettingInfo(CGM cgm, String deviceBody) {
		try {
			JsonNode root = new ObjectMapper().readTree(deviceBody);
			JsonNode device = root.path("records").get(0);
			String lastUpload = device.path("lastUploadDate").asText();

			OffsetDateTime uploadTime = OffsetDateTime.parse(lastUpload);
			OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
			boolean connected = !uploadTime.plusHours(6).isBefore(now);

			Integer max = 250, min = 70;
			JsonNode alerts = device.path("alertSchedules").get(0).path("alertSettings");
			for (JsonNode alert : alerts) {
				if ("high".equals(alert.path("alertName").asText())) {
					max = alert.path("value").asInt();
				} else if ("low".equals(alert.path("alertName").asText())) {
					min = alert.path("value").asInt();
				}
			}
			cgm.setIsConnected(connected ? "connected" : "disconnected");
			cgm.setLastEgvTime(uploadTime.toLocalDateTime());
			cgm.setMinGlucose(min);
			cgm.setMaxGlucose(max);

		} catch (Exception e) {
			log.error("saveDexcomSettingInfo: 파싱 실패", e);
			throw new BusinessException(GlobalErrorCodes.DEXCOM_JSON_PARSE_ERROR);
		}
	}

	public ResponseEntity<CGMDto> getDexcomInfo(Long userId) {
		CGM cgm = cgmRepository.findByUserId(userId)
			.orElseThrow(() -> new BusinessException(GlobalErrorCodes.DEXCOM_NOT_FOUND));
		return ResponseEntity.ok(CGMConverter.EntityToDto(cgm));
	}

	@Transactional
	public String updateMinMaxGlucose(Long userId, Integer min, Integer max) {
		CGM cgm = cgmRepository.findByUserId(userId)
			.orElseThrow(() -> {
				log.error("updateMinMaxGlucose: 사용자 {} 의 CGM 정보 없음", userId);
				return new BusinessException(GlobalErrorCodes.DEXCOM_NOT_FOUND);
			});

		boolean updated = false;
		if (min != null && min > 0) {
			cgm.setMinGlucose(min);
			updated = true;
		}
		if (max != null && max > 0) {
			cgm.setMaxGlucose(max);
			updated = true;
		}

		if (!updated) {
			log.warn("updateMinMaxGlucose: 유효하지 않은 min/max 입력 (userId={}, min={}, max={})", userId, min, max);
			throw new BusinessException(GlobalErrorCodes.DEXCOM_INVALID_MIN_MAX);
		}

		cgmRepository.save(cgm);
		log.info("updateMinMaxGlucose: 사용자 {} 의 min={}, max={} 업데이트 완료", userId, cgm.getMinGlucose(), cgm.getMaxGlucose());
		return "success";
	}

}
