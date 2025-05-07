package com.example.demo.convert;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.demo.dto.GlucoseDto;
import com.example.demo.entity.Dexcom;
import com.example.demo.entity.Glucose;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GlucoseConverter {
	private static final ObjectMapper objectMapper = new ObjectMapper();

	public static List<Glucose> fromDexcomJson(Dexcom dexcom, String jsonResponse) {
		List<Glucose> glucoseList = new ArrayList<>();

		try {
			JsonNode root = objectMapper.readTree(jsonResponse);
			JsonNode records = root.get("records");

			if (records.isArray()) {
				for (JsonNode record : records) {
					LocalDateTime displayTime = OffsetDateTime
						.parse(record.get("displayTime").asText())
						.toLocalDateTime();

					glucoseList.add(new Glucose(
						dexcom,
						record.get("value").asInt(),
						record.get("transmitterGeneration").asText(),
						record.get("trend").asText(),
						displayTime));
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Dexcom JSON 파싱 실패", e);
		}
		return glucoseList;
	}

	public static GlucoseDto EntityToDto(Glucose glucose) {
		return GlucoseDto.builder()
			.value(glucose.getValue())
			.displayApp(glucose.getTransmitterGeneration())
			.trend(glucose.getTrend())
			.recordedAt(glucose.getRecordedAt())
			.build();
	}
}
