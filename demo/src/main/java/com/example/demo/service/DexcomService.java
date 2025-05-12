package com.example.demo.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

import com.example.demo.common.error.GlobalErrorCodes;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.entity.DexcomAuth;
import com.example.demo.repository.DexcomAuthRepository;

import org.springframework.context.annotation.Lazy;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DexcomService {

	private final DexcomConfig dexcomConfig;
	private final DexcomRepository dexcomRepository;
	private final DexcomAuthRepository dexcomAuthRepository;

	private final RestTemplate restTemplate = new RestTemplate();
	private final DexcomAuthService dexcomAuthService;

	@Transactional
	public Dexcom saveDexcomSettingInfo(Long userId, String responseBody) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(responseBody);
			JsonNode records = root.get("records");

			if (records == null) {
				log.error("Dexcom 응답에 'records' 필드가 없습니다. responseBody={}", responseBody);
				throw new BusinessException(GlobalErrorCodes.DEXCOM_RECORDS_MISSING);
			}
			if (!records.isArray() || records.isEmpty()) {
				log.error("Dexcom 'records' 배열이 비어 있거나 잘못된 형식입니다. records={}", records);
				throw new BusinessException(GlobalErrorCodes.DEXCOM_RECORDS_EMPTY);
			}
			JsonNode device = records.get(0);
			String lastUpload = device.get("lastUploadDate").asText();
			if (lastUpload == null) {
				log.error("'lastUploadDate' 필드가 없습니다. device={}", device);
				throw new BusinessException(GlobalErrorCodes.DEXCOM_MISSING_LAST_UPLOAD);
			}
			OffsetDateTime uploadTime;

			try {
				uploadTime = OffsetDateTime.parse(lastUpload);
			} catch (DateTimeParseException e) {
				log.error("'lastUploadDate' 파싱 실패: {}", lastUpload, e);
				throw new BusinessException(GlobalErrorCodes.DEXCOM_LAST_UPLOAD_PARSE_ERROR);
			}
			OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

			String isConnected = uploadTime.plusHours(6).isBefore(now) ? "disconnected" : "connected";
			log.info("마지막 업로드 시간: {}, 연결 상태: {}", uploadTime, isConnected);

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
			LocalDateTime lastEgvTime = uploadTime.toLocalDateTime();

			Integer finalMaxGlucose = maxGlucose;
			Integer finalMinGlucose = minGlucose;
			Dexcom dexcom = dexcomRepository.findByUserId(userId)
				.map(existing -> {
					existing.setIsConnected(isConnected);
					existing.setLastEgvTime(lastEgvTime);
					return existing;
				})
				.orElseGet(() -> DexcomConverter.createInfo(userId, isConnected, finalMaxGlucose, finalMinGlucose, lastEgvTime));

			dexcomRepository.save(dexcom);

			log.info("Dexcom 설정 정보 저장 완료: 상태={}, max={}, min={}, time={}", isConnected, maxGlucose, minGlucose, lastEgvTime);
			return dexcom;
		} catch (Exception e) {
			log.error("Dexcom 설정 정보 파싱 실패", e);
			throw new BusinessException(GlobalErrorCodes.DEXCOM_JSON_PARSE_ERROR);
		}
	}

	@Transactional
	public String updateDeviceInfo(Long dexcomId) {

		DexcomAuth auth = dexcomAuthRepository.findByDexcomId(dexcomId)
			.orElseThrow(() -> {
				log.error("refreshAccessToken: DexcomAuth 정보 없음 (dexcomId={})", dexcomId);
				return new BusinessException(GlobalErrorCodes.DEXCOM_AUTH_NOT_FOUND);
			});

		if (auth.getExpiresIn() == null || auth.getExpiresIn().isBefore(LocalDateTime.now())) {
			log.info("accessToken 만료됨. 갱신 시도 (dexcomId={})", dexcomId);
			dexcomAuthService.refreshAccessToken(auth.getDexcomId());
			auth = dexcomAuthRepository.findByDexcomId(dexcomId)
				.orElseThrow(() -> {
					log.error("refreshAccessToken: DexcomAuth 정보 없음 (dexcomId={})", dexcomId);
					return new BusinessException(GlobalErrorCodes.DEXCOM_AUTH_NOT_FOUND);
				});
		} else {
			log.info("accessToken 유효함 (만료 시각: {})", auth.getExpiresIn());
		}

		Dexcom dexcom = auth.getDexcom();
		String accessToken = auth.getAccessToken();

		if (accessToken == null) {
			log.warn("updateDeviceInfo: accessToken 없음 (dexcomId={})", dexcom.getDexcomId());
			throw new BusinessException(GlobalErrorCodes.DEXCOM_NO_ACCESS_TOKEN);
		}

		String url = dexcomConfig.getDEVICE_ENDPOINT();
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Void> request = new HttpEntity<>(headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(
				url, HttpMethod.GET, request, String.class
			);
			log.info("updateDeviceInfo: Dexcom API 응답 status={}, body={}",
				response.getStatusCode(), response.getBody());

			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(response.getBody());
			JsonNode records = root.get("records");

			if (records == null || !records.isArray()) {
				log.error("updateDeviceInfo: 'records' 필드 누락 또는 잘못된 형식, root={}", root);
				throw new BusinessException(GlobalErrorCodes.DEXCOM_DEVICE_RECORDS_INVALID);
			}
			if (records.isEmpty()) {
				log.warn("updateDeviceInfo: records 배열이 비어 있음");
				dexcom.setIsConnected("unknown");
				dexcomRepository.save(dexcom);
				return "기기 정보가 부족합니다.";
			}

			String lastUploadDateStr = records.get(0).path("lastUploadDate").asText("");
			if (lastUploadDateStr.isEmpty()) {
				log.warn("updateDeviceInfo: lastUploadDate 누락, record={}", records.get(0));
				dexcom.setIsConnected("unknown");
				dexcomRepository.save(dexcom);
				return "기기 정보가 부족합니다.";
			}

			OffsetDateTime uploadTime;
			try {
				uploadTime = OffsetDateTime.parse(lastUploadDateStr);
			} catch (DateTimeParseException e) {
				log.error("updateDeviceInfo: lastUploadDate 파싱 실패: {}", lastUploadDateStr, e);
				throw new BusinessException(GlobalErrorCodes.DEXCOM_DEVICE_DATE_PARSE_ERROR);
			}

			OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
			boolean connected = !uploadTime.plusHours(6).isBefore(now);

			dexcom.setIsConnected(connected ? "connected" : "disconnected");
			dexcomRepository.save(dexcom);

			log.info("updateDeviceInfo: 사용자 기기 {} 기기 연결 상태={} (lastUpload={})",
				dexcomId, dexcom.getIsConnected(), uploadTime);
			return connected
				? "덱스콤에 연결되어 있습니다."
				: "덱스콤에 연결되어 있지 않습니다.";

		} catch (BusinessException be) {
			throw be;
		} catch (Exception e) {
			log.error("updateDeviceInfo: 알 수 없는 오류 발생", e);
			throw new BusinessException(GlobalErrorCodes.DEXCOM_DEVICE_UPDATE_FAILED);
		}
	}

	@Transactional
	public String updateMinMaxGlucose(Long userId, Integer min, Integer max) {
		Dexcom dexcom = dexcomRepository.findByUserId(userId)
			.orElseThrow(() -> {
				log.error("updateMinMaxGlucose: 사용자 {} 의 Dexcom 정보 없음", userId);
				return new BusinessException(GlobalErrorCodes.DEXCOM_NOT_FOUND);
			});

		boolean updated = false;
		if (min != null && min > 0) {
			dexcom.setMinGlucose(min);
			updated = true;
		}
		if (max != null && max > 0) {
			dexcom.setMaxGlucose(max);
			updated = true;
		}

		if (!updated) {
			log.warn("updateMinMaxGlucose: 유효하지 않은 min/max 입력 (userId={}, min={}, max={})",
				userId, min, max);
			throw new BusinessException(GlobalErrorCodes.DEXCOM_INVALID_MIN_MAX);
		}

		dexcomRepository.save(dexcom);
		log.info("updateMinMaxGlucose: 사용자 {} 의 min={}, max={} 업데이트 완료",
			userId, dexcom.getMinGlucose(), dexcom.getMaxGlucose());
		return "success";
	}

	public ResponseEntity<DexcomDto> getDexcomInfo(Long userId) {
		Dexcom dexcom = dexcomRepository.findByUserId(userId)
			.orElseThrow(() -> {
				log.error("getDexcomInfo: 사용자 {} 의 Dexcom 정보 없음", userId);
				return new BusinessException(GlobalErrorCodes.DEXCOM_NOT_FOUND);
			});

		DexcomDto dto = DexcomConverter.EntityToDto(dexcom);
		log.info("getDexcomInfo: 사용자 {} 정보 조회 완료: {}", userId, dto);
		return ResponseEntity.ok(dto);
	}

}
