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
import com.example.demo.entity.Glucose;
import com.example.demo.repository.DexcomAuthRepository;
import com.example.demo.repository.DexcomRepository;
import com.example.demo.repository.GlucoseRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GlucoseService {

	private final GlucoseRepository glucoseRepository;
	private final DexcomRepository dexcomRepository;
	private final DexcomAuthRepository dexcomAuthRepository;
	private final DexcomConfig dexcomConfig;

	private final RestTemplate restTemplate = new RestTemplate();
	private final DateTimeFormatter isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	@Transactional
	public String saveEgvs(Long dexcomId) {
		String accessToken = dexcomAuthRepository.getAccessTokenByDexcomId(dexcomId);

		Dexcom dexcom = dexcomRepository.findById(dexcomId)
			.orElseThrow(() -> new RuntimeException("Dexcom 정보 없음"));

		if (accessToken == null) {
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

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
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

	public List<GlucoseDto> getMyEgvs(Long dexcomId, String startDate, String endDate) {
		DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
		LocalDateTime start = LocalDateTime.parse(startDate, formatter);
		LocalDateTime end = LocalDateTime.parse(endDate, formatter);

		if(startDate == null || endDate == null) {
			return null;
		}

		return glucoseRepository.findByDexcomIdAndTimeBetween(dexcomId, start, end).stream()
			.map(GlucoseConverter::EntityToDto)
			.toList();
	}

	public List<GlucoseDto> getMyAllEgvs(Long dexcomId) {
		return glucoseRepository.findByDexcomId(dexcomId).stream()
			.map(GlucoseConverter::EntityToDto)
			.toList();
	}

	public List<Glucose> fetchAndFilterNewGlucose(Dexcom dexcom) {
		String accessToken = dexcomAuthRepository.getAccessTokenByDexcomId(dexcom.getDexcomId());
		if (accessToken == null) return List.of();

		LocalDateTime start = dexcom.getLastEgvTime() != null ? dexcom.getLastEgvTime() : LocalDateTime.now().minusHours(1);
		LocalDateTime end = LocalDateTime.now();

		String url = dexcomConfig.getEGV_ENDPOINT() +
			"?startDate=" + isoFormatter.format(start) +
			"&endDate="   + isoFormatter.format(end);

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
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


	@Transactional
	public String saveEgvsWithPeriod(Long dexcomId, String startDate, String endDate) {
		// 1) 액세스 토큰 & Dexcom 엔티티 가져오기
		String accessToken = dexcomAuthRepository.getAccessTokenByDexcomId(dexcomId);
		Dexcom dexcom = dexcomRepository.findById(dexcomId)
			.orElseThrow(() -> new RuntimeException("Dexcom 정보 없음"));

		if (accessToken == null) {
			return "유효하지 않거나 엑세스 토큰이 없습니다.";
		}

		// 2) 파라미터로 받은 기간 파싱 (예외 던지거나 메시지 반환해도 좋습니다)
		LocalDateTime start = LocalDateTime.parse(startDate, isoFormatter);
		LocalDateTime end   = LocalDateTime.parse(endDate,   isoFormatter);

		// 3) Dexcom EGV API 호출
		String url = dexcomConfig.getEGV_ENDPOINT()
			+ "?startDate=" + isoFormatter.format(start)
			+ "&endDate="   + isoFormatter.format(end);

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Void> request = new HttpEntity<>(headers);

		ResponseEntity<String> resp = restTemplate.exchange(
			url, HttpMethod.GET, request, String.class
		);

		log.info("Dexcom raw body: {}", resp.getBody());

		try {
			// 4) JSON → 엔티티 리스트 변환
			List<Glucose> glucoseList = GlucoseConverter.fromDexcomJson(dexcom, resp.getBody());

			// 5) 만약 lastEgvTime을 업데이트하고 싶다면 가장 최신값으로 덮어쓰기
			glucoseList.stream()
				.map(Glucose::getRecordedAt)
				.max(LocalDateTime::compareTo)
				.ifPresent(dexcom::setLastEgvTime);

			// 6) DB에 저장
			glucoseRepository.saveAll(glucoseList);

			return String.format("혈당 데이터 저장 완료 (%s ~ %s)", startDate, endDate);
		} catch (Exception e) {
			log.error("혈당 데이터 저장 실패", e);
			return "혈당 데이터 저장 실패";
		}
	}
}

