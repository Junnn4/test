package com.example.demo.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import com.example.demo.common.util.TimeUtil;
import com.example.demo.convert.GlucoseConverter;
import com.example.demo.dto.GlucoseDto;
import com.example.demo.entity.CGM;
import com.example.demo.entity.Glucose;
import com.example.demo.entity.GlucoseLevel;
import com.example.demo.repository.CGMRepository;
import com.example.demo.repository.GlucoseRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GlucoseService {

	private final DexcomConfig dexcomConfig;
	private final GlucoseRepository glucoseRepository;
	private final CGMService cgmService;
	private final CGMRepository cgmRepository;

	private final RestTemplate restTemplate = new RestTemplate();
	private final DateTimeFormatter isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	@Transactional
	public String saveEgvs(Long dexcomId) {
		CGM cgm = cgmRepository.findByDexcomId(dexcomId)
			.orElseThrow(() -> {
				log.error("saveEgvs: DexcomAuth 정보 없음 (dexcomId={})", dexcomId);
				return new BusinessException(GlobalErrorCodes.DEXCOM201_NO_AUTH);
			});

		if (cgm.getExpiresIn() == null || cgm.getExpiresIn().isBefore(LocalDateTime.now())) {
			log.info("accessToken 만료됨. 갱신 시도 (dexcomId={})", dexcomId);
			cgmService.refreshAccessToken(cgm.getDexcomId());
			cgm = cgmRepository.findByDexcomId(dexcomId)
				.orElseThrow(() -> {
					log.error("refreshAccessToken: DexcomAuth 정보 없음 (dexcomId={})", dexcomId);
					return new BusinessException(GlobalErrorCodes.DEXCOM_AUTH_NOT_FOUND);
				});
		} else {
			log.info("accessToken 유효함 (만료 시각: {})", cgm.getExpiresIn());
		}

		String token = cgm.getAccessToken();
		if (token == null) {
			log.warn("saveEgvs: access_token 없음 (dexcomId={})", dexcomId);
			throw new BusinessException(GlobalErrorCodes.DEXCOM202_INVALID_TOKEN);
		}

		LocalDateTime end = LocalDateTime.now();
		LocalDateTime start = (cgm.getLastEgvTime() != null)
			? cgm.getLastEgvTime()
			: end.minusHours(1);

		log.info("saveEgvs: 요청 기간 {} ~ {}", isoFormatter.format(start), isoFormatter.format(end));

		List<LocalDateTime> existingTimes = glucoseRepository.findTimesByDexcomIdAndTimeRange(dexcomId, start, end);
		DateTimeFormatter minuteFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
		Set<String> existingTimeSet = existingTimes.stream()
			.map(minuteFormatter::format)
			.collect(Collectors.toSet());
		log.info("기존 저장된 recordedAt (분 단위): {}", existingTimeSet);

		String url = dexcomConfig.getEGV_ENDPOINT()
			+ "?startDate=" + isoFormatter.format(start)
			+ "&endDate=" + isoFormatter.format(end);

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
			List<Glucose> parsed = GlucoseConverter.fromDexcomJson(cgm, raw);

			List<Glucose> filtered = parsed.stream()
				.filter(g -> !existingTimeSet.contains(minuteFormatter.format(g.getRecordedAt())))
				.toList();

			List<String> filteredTimes = filtered.stream()
				.map(g -> minuteFormatter.format(g.getRecordedAt()))
				.toList();
			log.info("신규 저장 대상 recordedAt 목록 (분 단위): {}", filteredTimes);

			if (!filtered.isEmpty()) {
				glucoseRepository.saveAll(filtered);
				filtered.stream()
					.map(Glucose::getRecordedAt)
					.max(LocalDateTime::compareTo)
					.ifPresent(cgm::setLastEgvTime);
			} else {
				log.info("saveEgvs: 저장할 데이터가 없습니다. ({} ~ {})", start, end);
			}

			return String.format("총 %d건의 혈당 데이터를 저장했습니다. (%s ~ %s)", filtered.size(), isoFormatter.format(start), isoFormatter.format(end));
		} catch (Exception e) {
			log.error("saveEgvs: 혈당 데이터 파싱 실패 (dexcomId={})", dexcomId, e);
			throw new BusinessException(GlobalErrorCodes.DEXCOM204_JSON_PARSE_FAILED);
		}
	}

	@Transactional
	public String saveEgvsWithPeriod(Long dexcomId, String startDate, String endDate) {
		CGM cgm = cgmRepository.findByDexcomId(dexcomId)
			.orElseThrow(() -> {
				log.error("saveEgvsWithPeriod: DexcomAuth 정보 없음 (dexcomId={})", dexcomId);
				return new BusinessException(GlobalErrorCodes.DEXCOM201_NO_AUTH);
			});

		if (cgm.getExpiresIn() == null || cgm.getExpiresIn().isBefore(LocalDateTime.now())) {
			log.info("accessToken 만료됨. 갱신 시도 (dexcomId={})", dexcomId);
			cgmService.refreshAccessToken(cgm.getDexcomId());
			cgm = cgmRepository.findByDexcomId(dexcomId)
				.orElseThrow(() -> {
					log.error("refreshAccessToken: DexcomAuth 정보 없음 (dexcomId={})", dexcomId);
					return new BusinessException(GlobalErrorCodes.DEXCOM_AUTH_NOT_FOUND);
				});
		} else {
			log.info("accessToken 유효함 (만료 시각: {})", cgm.getExpiresIn());
		}

		String token = cgm.getAccessToken();
		if (token == null) {
			log.warn("saveEgvsWithPeriod: access_token 없음 (dexcomId={})", dexcomId);
			throw new BusinessException(GlobalErrorCodes.DEXCOM202_INVALID_TOKEN);
		}

		LocalDateTime startKCT = LocalDateTime.parse(startDate, isoFormatter);
		LocalDateTime endKCT = LocalDateTime.parse(endDate, isoFormatter);
		LocalDateTime start = TimeUtil.toUTCLocalDateTime(startDate);
		LocalDateTime end = TimeUtil.toUTCLocalDateTime(endDate);
		log.info("startDate :{}  ~ endDate :{}", startDate, endDate);
		log.info("start :{}  ~ end :{}", start, end);

		DateTimeFormatter minuteFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

		List<LocalDateTime> existingTimes = glucoseRepository.findTimesByDexcomIdAndTimeRange(dexcomId, startKCT, endKCT);
		Set<String> existingTimeSet = existingTimes.stream()
			.map(minuteFormatter::format)
			.collect(Collectors.toSet());

		log.info("기존 저장된 recordedAt (분 단위): {}", existingTimeSet);

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(token);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Void> request = new HttpEntity<>(headers);

		List<Glucose> allToSave = new ArrayList<>();
		LocalDateTime cursor = start;

		while (cursor.isBefore(end)) {
			LocalDateTime next = cursor.plusHours(12).isAfter(end) ? end : cursor.plusHours(12);
			log.info("saveEgvsWithPeriod: 요청 기간 {} ~ {}", cursor, next);

			String url = dexcomConfig.getEGV_ENDPOINT()
				+ "?startDate=" + isoFormatter.format(cursor)
				+ "&endDate=" + isoFormatter.format(next);

			String raw;
			try {
				raw = restTemplate.exchange(url, HttpMethod.GET, request, String.class).getBody();
			} catch (Exception e) {
				log.error("saveEgvsWithPeriod: 혈당 데이터 조회 실패 ({}~{})", cursor, next, e);
				throw new BusinessException(GlobalErrorCodes.DEXCOM203_EGV_FETCH_FAILED);
			}

			List<Glucose> parsed;
			try {
				parsed = GlucoseConverter.fromDexcomJson(cgm, raw);
			} catch (Exception e) {
				log.error("saveEgvsWithPeriod: 혈당 데이터 파싱 실패 ({}~{})", cursor, next, e);
				throw new BusinessException(GlobalErrorCodes.DEXCOM204_JSON_PARSE_FAILED);
			}

			List<Glucose> filtered = parsed.stream()
				.filter(g -> !existingTimeSet.contains(minuteFormatter.format(g.getRecordedAt())))
				.toList();

			List<String> filteredTimes = filtered.stream()
				.map(g -> minuteFormatter.format(g.getRecordedAt()))
				.toList();
			log.info("신규 저장 대상 recordedAt 목록 (분 단위): {}", filteredTimes);

			allToSave.addAll(filtered);

			filtered.stream()
				.map(Glucose::getRecordedAt)
				.map(minuteFormatter::format)
				.forEach(existingTimeSet::add);

			cursor = next;
		}

		if (!allToSave.isEmpty()) {
			glucoseRepository.saveAll(allToSave);
			allToSave.stream()
				.map(Glucose::getRecordedAt)
				.max(LocalDateTime::compareTo)
				.ifPresent(cgm::setLastEgvTime);
		} else {
			log.info("saveEgvsWithPeriod: 저장할 데이터가 없습니다. ({} ~ {})", start, end);
		}

		return String.format("총 %d건의 혈당 데이터를 저장했습니다. (%s ~ %s)", allToSave.size(), startDate, endDate);
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
				.findByCgm_DexcomIdAndRecordedAtBetween(dexcomId, start, end);
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
			List<Glucose> entities = glucoseRepository.findByCgm_DexcomId(dexcomId);
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
			List<Glucose> list = glucoseRepository.findByCgm_DexcomId(dexcomId);
			return list.stream()
				.map(g -> GlucoseLevel.getLevel(g.getValue()))
				.collect(Collectors.groupingBy(level -> level, Collectors.counting()));
		} catch (Exception e) {
			log.error("getGlucoseLevelCounts: DB 조회 실패 (dexcomId={})", dexcomId, e);
			throw new BusinessException(GlobalErrorCodes.INTERNAL_SERVER_ERROR);
		}
	}
}