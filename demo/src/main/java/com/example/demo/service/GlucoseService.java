package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.convert.GlucoseConverter;
import com.example.demo.entity.Dexcom;
import com.example.demo.entity.Glucose;
import com.example.demo.repository.DexcomRepository;
import com.example.demo.repository.GlucoseRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GlucoseService {

	private final GlucoseRepository glucoseRepository;
	private final DexcomRepository dexcomRepository;

	public void saveGlucoseFromDexcom(String jsonResponse, Long dexcomId) {
		try {
			Dexcom dexcom = dexcomRepository.findById(dexcomId)
				.orElseThrow(() -> new RuntimeException("Dexcom 정보 없음"));

			List<Glucose> glucoseList = GlucoseConverter.fromDexcomJson(dexcom, jsonResponse);

			glucoseRepository.saveAll(glucoseList);

		} catch (Exception e) {
			log.error("❌ 혈당 데이터 저장 실패", e);
			throw new RuntimeException("혈당 데이터 저장 실패", e);
		}
	}
}

