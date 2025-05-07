package com.example.demo.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.demo.common.DexcomConfig;
import com.example.demo.convert.GlucoseConverter;
import com.example.demo.dto.GlucoseDto;
import com.example.demo.entity.Dexcom;
import com.example.demo.entity.DexcomAuth;
import com.example.demo.entity.Glucose;
import com.example.demo.repository.DexcomAuthRepository;
import com.example.demo.repository.GlucoseRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GlucoseService {

	private final DexcomConfig dexcomConfig;
	private final DexcomAuthRepository dexcomAuthRepository;
	private final GlucoseRepository glucoseRepository;

	private final RestTemplate restTemplate = new RestTemplate();
	private final DateTimeFormatter isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	@Transactional
	public String saveEgvs(Long dexcomId) {
		DexcomAuth dexcomAuth = dexcomAuthRepository.findById(dexcomId)
			.orElseThrow(()-> new RuntimeException("DexcomAuth 정보 없음"));

		String token = dexcomAuth.getAccessToken();
		Dexcom dexcom = dexcomAuth.getDexcom();

		if (token == null) {
			log.warn("access_token 없음. 먼저 인증하세요.");
			return "유효하지 않거나 엑세스 토큰이 없습니다.";
		}

		LocalDateTime end = LocalDateTime.now();

		LocalDateTime start = (dexcom.getLastEgvTime() != null)
			? dexcom.getLastEgvTime()
			: end.minusHours(1);

		String url = dexcomConfig.getEGV_ENDPOINT() +
			"?startDate=" + isoFormatter.format(start) +
			"&endDate="   + isoFormatter.format(end);

		log.info("시작 날짜 : {} , 종료 날짜 : {}", isoFormatter.format(start), isoFormatter.format(end));

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(token);
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<Void> request = new HttpEntity<>(headers);
		ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

		log.info("Dexcom raw body: {}", resp.getBody());

		try {
			List<Glucose> glucoseList = GlucoseConverter.fromDexcomJson(dexcom, resp.getBody());

			Optional<LocalDateTime> latestEgvTime = glucoseList.stream()
				.map(Glucose::getRecordedAt)
				.max(LocalDateTime::compareTo);

			latestEgvTime.ifPresent(dexcom::setLastEgvTime);

			glucoseRepository.saveAll(glucoseList);

			return "혈당 데이터 저장 완료";
		} catch (Exception e) {
			log.error("혈당 데이터 저장 실패", e);
			return "혈당 데이터 저장 실패";
		}
	}

	@Transactional
	public String saveEgvsWithPeriod(Long dexcomId, String startDate, String endDate) {
		DexcomAuth dexcomAuth = dexcomAuthRepository.findById(dexcomId)
			.orElseThrow(() -> new RuntimeException("DexcomAuth 정보 없음"));

		String token = dexcomAuth.getAccessToken();
		Dexcom dexcom = dexcomAuth.getDexcom();

		if (token == null) {
			return "유효하지 않거나 엑세스 토큰이 없습니다.";
		}

		LocalDateTime start = LocalDateTime.parse(startDate, isoFormatter);
		LocalDateTime end = LocalDateTime.parse(endDate, isoFormatter);

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(token);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Void> request = new HttpEntity<>(headers);

		int totalSaved = 0;
		LocalDateTime cursor = start;

		while (cursor.isBefore(end)) {
			LocalDateTime next = cursor.plusHours(12);
			if (next.isAfter(end)) {
				next = end;
			}

			String url = dexcomConfig.getEGV_ENDPOINT()
				+ "?startDate=" + isoFormatter.format(cursor)
				+ "&endDate=" + isoFormatter.format(next);

			log.info("⏱ 요청: {} ~ {}", cursor, next);

			ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
			String responseBody = resp.getBody();
			log.debug("📨 응답 데이터: {}", responseBody);

			List<Glucose> glucoseList;
			try {
				glucoseList = GlucoseConverter.fromDexcomJson(dexcom, responseBody);
			} catch (Exception e) {
				log.error("❌ JSON 파싱 실패", e);
				break;
			}

			if (glucoseList.isEmpty()) {
				log.warn("📭 응답 데이터 없음. 중단: {} ~ {}", cursor, next);
				break;
			}

			// 최신 시간 저장
			glucoseList.stream()
				.map(Glucose::getRecordedAt)
				.max(LocalDateTime::compareTo)
				.ifPresent(dexcom::setLastEgvTime);

			glucoseRepository.saveAll(glucoseList);
			totalSaved += glucoseList.size();
			cursor = next;
		}

		return String.format("총 %d건의 혈당 데이터를 저장했습니다. (%s ~ %s)", totalSaved, startDate, endDate);
	}


	public List<GlucoseDto> getMyEgvs(Long dexcomId, String startDate, String endDate) {
		DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
		LocalDateTime start = LocalDateTime.parse(startDate, formatter);
		LocalDateTime end = LocalDateTime.parse(endDate, formatter);

		if(startDate == null || endDate == null) {
			return null;
		}

		return glucoseRepository.findByDexcom_DexcomIdAndRecordedAtBetween(dexcomId, start, end).stream()
			.map(GlucoseConverter::EntityToDto)
			.toList();
	}

	public List<GlucoseDto> getMyAllEgvs(Long dexcomId) {
		return glucoseRepository.findByDexcom_DexcomId(dexcomId).stream()
			.map(GlucoseConverter::EntityToDto)
			.toList();
	}

	public List<Glucose> fetchAndFilterNewGlucose(Dexcom dexcom) {
		String token = dexcomAuthRepository.findByDexcomId(dexcom.getDexcomId())
			.orElseThrow(() -> new RuntimeException("토큰 정보 없음"))
			.getAccessToken();

		if (token == null) return List.of();

		LocalDateTime start = dexcom.getLastEgvTime() != null ? dexcom.getLastEgvTime() : LocalDateTime.now().minusHours(1);
		LocalDateTime end = LocalDateTime.now();

		String url = dexcomConfig.getEGV_ENDPOINT() +
			"?startDate=" + isoFormatter.format(start) +
			"&endDate="   + isoFormatter.format(end);

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(token);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Void> request = new HttpEntity<>(headers);

		ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
		List<Glucose> glucoseList = GlucoseConverter.fromDexcomJson(dexcom, resp.getBody());

		if (glucoseList.isEmpty()) return List.of();

		// 중복 제거용: 한 번에 기존 시간 조회
		List<LocalDateTime> existingTimes = glucoseRepository.findTimesByDexcomIdAndTimeIn(
			dexcom.getDexcomId(),
			glucoseList.stream().map(Glucose::getRecordedAt).toList()
		);

		return glucoseList.stream()
			.filter(glucose -> !existingTimes.contains(glucose.getRecordedAt()))
			.toList();
	}
}

