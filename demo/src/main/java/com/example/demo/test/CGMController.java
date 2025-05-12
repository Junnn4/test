package com.example.demo.test;

import com.example.demo.common.DexcomConfig;
import com.example.demo.dto.CGMDto;
import com.example.demo.dto.GlucoseDto;
import com.example.demo.service.CGMService;
import com.example.demo.service.GlucoseService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/glucose")
@CrossOrigin(origins = "*")
public class CGMController {

	private final CGMService cgmService;
	private final GlucoseService glucoseService;
	private final DexcomConfig dexcomConfig;

	// ======================= [CGM] Dexcom 토큰 및 연결 설정 =======================

	@GetMapping("/auth")
	public void authorize(HttpServletResponse response) throws IOException {
		String state = UUID.randomUUID().toString();

		String url = UriComponentsBuilder
			.fromHttpUrl(dexcomConfig.getAUTHORIZATION_ENDPOINT())
			.queryParam("client_id", dexcomConfig.getClientId())
			.queryParam("redirect_uri", dexcomConfig.getRedirectUri())
			.queryParam("response_type", "code")
			.queryParam("scope", "offline_access")
			.queryParam("state", state)
			.toUriString();

		log.info("Redirecting to Dexcom OAuth: {}", url);
		response.sendRedirect(url);
	}

	@GetMapping("/code")
	public ResponseEntity<String> handleOAuthCallback(
		@RequestParam Map<String, String> params) {
		Long userId = 1L;
		String result = cgmService.exchangeCodeToToken(params, userId);
		return ResponseEntity.ok(result);
	}

	@PostMapping("/token/refresh")
	public ResponseEntity<String> refreshToken(
		@RequestParam Long dexcomId) {
		String result = cgmService.refreshAccessToken(dexcomId);
		return ResponseEntity.ok(result);
	}

	@PutMapping("/device")
	public ResponseEntity<String> updateDeviceInfo(
		@RequestParam Long dexcomId) {
		String result = cgmService.updateDeviceInfo(dexcomId);
		return ResponseEntity.ok(result);
	}

	@GetMapping("/info")
	public ResponseEntity<CGMDto> getInfo(
		@RequestParam Long userId) {
		return cgmService.getDexcomInfo(userId);
	}

	@PatchMapping("/range")
	public ResponseEntity<String> updateGlucoseRange(
		@RequestParam Long userId,
		@RequestParam(required = false) Integer min,
		@RequestParam(required = false) Integer max) {
		String result = cgmService.updateMinMaxGlucose(userId, min, max);
		return ResponseEntity.ok(result);
	}

	// ======================= [GLUCOSE] 혈당 측정 관련 =======================

	/**
	 * 마지막 측정 이후 ~ 현재까지의 혈당 데이터를 저장 (중복 제외)
	 */
	@PostMapping("/glucose")
	public ResponseEntity<String> saveEgvs(
		@RequestParam Long dexcomId) {
		String result = glucoseService.saveEgvs(dexcomId);
		return ResponseEntity.ok(result);
	}

	/**
	 * 지정된 기간의 혈당 데이터를 저장 (중복 제외)
	 */
	@PostMapping("/period")
	public ResponseEntity<String> saveEgvsWithPeriod(
		@RequestParam Long dexcomId,
		@RequestParam String startDate,
		@RequestParam String endDate) {
		String result = glucoseService.saveEgvsWithPeriod(dexcomId, startDate, endDate);
		return ResponseEntity.ok(result);
	}

	/**
	 * 기간별 내 혈당 데이터 조회
	 */
	@GetMapping("/my")
	public ResponseEntity<List<GlucoseDto>> getMyEgvs(
		@RequestParam Long dexcomId,
		@RequestParam String startDate,
		@RequestParam String endDate) {
		List<GlucoseDto> result = glucoseService.getMyEgvs(dexcomId, startDate, endDate);
		return ResponseEntity.ok(result);
	}

	/**
	 * 전체 내 혈당 데이터 조회
	 */
	@GetMapping("/all")
	public ResponseEntity<List<GlucoseDto>> getMyAllEgvs(
		@RequestParam Long dexcomId) {
		List<GlucoseDto> result = glucoseService.getMyAllEgvs(dexcomId);
		return ResponseEntity.ok(result);
	}

	/**
	 * 혈당 구간별 통계 (LOW / NORMAL / HIGH)
	 */
	@GetMapping("/level")
	public ResponseEntity<Map<String, Long>> getGlucoseLevelCounts(
		@RequestParam Long dexcomId) {
		Map<String, Long> result = glucoseService.getGlucoseLevelCounts(dexcomId);
		return ResponseEntity.ok(result);
	}
}


