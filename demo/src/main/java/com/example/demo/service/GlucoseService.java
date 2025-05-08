package com.example.demo.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.demo.common.DexcomConfig;
import com.example.demo.common.error.GlobalErrorCodes;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.convert.GlucoseConverter;
import com.example.demo.dto.GlucoseDto;
import com.example.demo.entity.Dexcom;
import com.example.demo.entity.DexcomAuth;
import com.example.demo.entity.Glucose;
import com.example.demo.entity.GlucoseLevel;
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
		DexcomAuth auth = dexcomAuthRepository.findByDexcomId(dexcomId)
			.orElseThrow(() -> {
				log.error("saveEgvs: DexcomAuth 정보 없음 (dexcomId={})", dexcomId);
				return new BusinessException(GlobalErrorCodes.DEXCOM201_NO_AUTH);
			});

		String token = auth.getAccessToken();
		if (token == null) {
			log.warn("saveEgvs: access_token 없음 (dexcomId={})", dexcomId);
			throw new BusinessException(GlobalErrorCodes.DEXCOM202_INVALID_TOKEN);
		}

		Dexcom dexcom = auth.getDexcom();
		LocalDateTime end   = LocalDateTime.now();
		LocalDateTime start = (dexcom.getLastEgvTime() != null)
			? dexcom.getLastEgvTime()
			: end.minusHours(1);

		log.info("saveEgvs: 요청 기간 {} ~ {}", isoFormatter.format(start), isoFormatter.format(end));

		String url = dexcomConfig.getEGV_ENDPOINT()
			+ "?startDate=" + isoFormatter.format(start)
			+ "&endDate="   + isoFormatter.format(end);

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(token);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Void> request = new HttpEntity<>(headers);

		String raw;
		try {
			ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
			raw = resp.getBody();
			log.info("saveEgvs: Dexcom raw body: {}", raw);
		} catch (Exception e) {
			log.error("saveEgvs: 혈당 데이터 조회 실패 (dexcomId={})", dexcomId, e);
			throw new BusinessException(GlobalErrorCodes.DEXCOM203_EGV_FETCH_FAILED);
		}

		try {
			List<Glucose> list = GlucoseConverter.fromDexcomJson(dexcom, raw);
			list.stream()
				.map(Glucose::getRecordedAt)
				.max(LocalDateTime::compareTo)
				.ifPresent(dexcom::setLastEgvTime);

			glucoseRepository.saveAll(list);
			return "혈당 데이터 저장 완료";
		} catch (Exception e) {
			log.error("saveEgvs: 혈당 데이터 파싱 실패 (dexcomId={})", dexcomId, e);
			throw new BusinessException(GlobalErrorCodes.DEXCOM204_JSON_PARSE_FAILED);
		}
	}

	@Transactional
	public String saveEgvsWithPeriod(Long dexcomId, String startDate, String endDate) {
		DexcomAuth auth = dexcomAuthRepository.findByDexcomId(dexcomId)
			.orElseThrow(() -> {
				log.error("saveEgvsWithPeriod: DexcomAuth 정보 없음 (dexcomId={})", dexcomId);
				return new BusinessException(GlobalErrorCodes.DEXCOM201_NO_AUTH);
			});

		String token = auth.getAccessToken();
		if (token == null) {
			log.warn("saveEgvsWithPeriod: access_token 없음 (dexcomId={})", dexcomId);
			throw new BusinessException(GlobalErrorCodes.DEXCOM202_INVALID_TOKEN);
		}

		Dexcom dexcom = auth.getDexcom();
		LocalDateTime start = LocalDateTime.parse(startDate, isoFormatter);
		LocalDateTime end   = LocalDateTime.parse(endDate,   isoFormatter);

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(token);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Void> request = new HttpEntity<>(headers);

		int saved = 0;
		LocalDateTime cursor = start;

		while (cursor.isBefore(end)) {
			LocalDateTime next = cursor.plusHours(12).isAfter(end) ? end : cursor.plusHours(12);
			log.info("saveEgvsWithPeriod: 요청 기간 {} ~ {}", cursor, next);

			String url = dexcomConfig.getEGV_ENDPOINT()
				+ "?startDate=" + isoFormatter.format(cursor)
				+ "&endDate="   + isoFormatter.format(next);

			String raw;
			try {
				raw = restTemplate.exchange(url, HttpMethod.GET, request, String.class).getBody();
			} catch (Exception e) {
				log.error("saveEgvsWithPeriod: 혈당 데이터 조회 실패 ({}~{})", cursor, next, e);
				throw new BusinessException(GlobalErrorCodes.DEXCOM203_EGV_FETCH_FAILED);
			}

			List<Glucose> list;
			try {
				list = GlucoseConverter.fromDexcomJson(dexcom, raw);
			} catch (Exception e) {
				log.error("saveEgvsWithPeriod: 혈당 데이터 파싱 실패 ({}~{})", cursor, next, e);
				throw new BusinessException(GlobalErrorCodes.DEXCOM204_JSON_PARSE_FAILED);
			}

			if (!list.isEmpty()) {
				glucoseRepository.saveAll(list);
				saved += list.size();
				list.stream()
					.map(Glucose::getRecordedAt)
					.max(LocalDateTime::compareTo)
					.ifPresent(dexcom::setLastEgvTime);
			} else {
				log.info("saveEgvsWithPeriod: 응답 데이터 없음 ({}~{})", cursor, next);
			}
			cursor = next;
		}
		return String.format("총 %d건의 혈당 데이터를 저장했습니다. (%s ~ %s)", saved, startDate, endDate);
	}

	public List<GlucoseDto> getMyEgvs(Long dexcomId, String startDate, String endDate) {
		log.info("getMyEgvs: dexcomId={}, startDate={}, endDate={}", dexcomId, startDate, endDate);
		DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
		LocalDateTime start = LocalDateTime.parse(startDate, formatter);
		LocalDateTime end = LocalDateTime.parse(endDate, formatter);

		if (startDate == null || endDate == null) {
			log.error("getMyEgvs: 기간 파라미터 누락");
			throw new BusinessException(GlobalErrorCodes.DEXCOM205_INVALID_PERIOD);
		}

		try {
			List<Glucose> entities = glucoseRepository
				.findByDexcom_DexcomIdAndRecordedAtBetween(dexcomId, start, end);
			return entities.stream()
				.map(GlucoseConverter::EntityToDto)
				.toList();
		} catch (Exception e) {
			log.error("getMyEgvs: DB 조회 실패 (dexcomId={}, {}~{})", dexcomId, startDate, endDate, e);
			throw new BusinessException(GlobalErrorCodes.INTERNAL_SERVER_ERROR);
		}
	}

	public List<GlucoseDto> getMyAllEgvs(Long dexcomId) {
		log.info("getMyAllEgvs: dexcomId={}", dexcomId);
		try {
			List<Glucose> entities = glucoseRepository.findByDexcom_DexcomId(dexcomId);
			return entities.stream()
				.map(GlucoseConverter::EntityToDto)
				.toList();
		} catch (Exception e) {
			log.error("getMyAllEgvs: DB 조회 실패 (dexcomId={})", dexcomId, e);
			throw new BusinessException(GlobalErrorCodes.INTERNAL_SERVER_ERROR);
		}
	}

	public Map<String, Long> getGlucoseLevelCounts(Long dexcomId) {
		log.info("getGlucoseLevelCounts: dexcomId={}", dexcomId);
		try {
			List<Glucose> list = glucoseRepository.findByDexcom_DexcomId(dexcomId);
			return list.stream()
				.map(g -> GlucoseLevel.getLevel(g.getValue()))
				.collect(Collectors.groupingBy(level -> level, Collectors.counting()));
		} catch (Exception e) {
			log.error("getGlucoseLevelCounts: DB 조회 실패 (dexcomId={})", dexcomId, e);
			throw new BusinessException(GlobalErrorCodes.INTERNAL_SERVER_ERROR);
		}
	}
}

