package com.example.demo.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.example.demo.convert.DexcomConverter;
import com.example.demo.entity.Dexcom;
import com.example.demo.entity.User;
import com.example.demo.repository.DexcomRepository;
import com.example.demo.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DexcomService {
	private final UserRepository userRepository;
	private final DexcomRepository dexcomRepository;

	public void saveDexcomSettingInfo(Long userId, String responseBody) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(responseBody);
			JsonNode records = root.get("records");

			if (!records.isArray() || records.isEmpty()) {
				throw new RuntimeException("기기 정보 없음");
			}

			JsonNode device = records.get(0);

			String isConnected = "connected";
			String lastUpload = device.get("lastUploadDate").asText();
			LocalDateTime lastEgvTime = LocalDateTime.parse(lastUpload, DateTimeFormatter.ISO_DATE_TIME);

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

			Dexcom dexcom = DexcomConverter.createInfo(userId, isConnected, maxGlucose, minGlucose, lastEgvTime);
			dexcomRepository.save(dexcom);

			log.info("✅ Dexcom 설정 정보 저장 완료: max={}, min={}, time={}", maxGlucose, minGlucose, lastEgvTime);

		} catch (Exception e) {
			log.error("Dexcom 설정 정보 파싱 실패", e);
			throw new RuntimeException("Dexcom 설정 정보 저장 실패", e);
		}
	}


}
