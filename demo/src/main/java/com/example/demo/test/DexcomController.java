package com.example.demo.test;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.example.demo.common.DexcomConfig;
import com.example.demo.dto.DexcomDto;
import com.example.demo.dto.GlucoseDto;
import com.example.demo.service.DexcomAuthService;
import com.example.demo.service.DexcomService;
import com.example.demo.service.GlucoseService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/glucose")
@CrossOrigin(origins = "*")
public class DexcomController {
	private static final Logger log = LoggerFactory.getLogger(DexcomController.class);

	private final DexcomService dexcomService;
	private final DexcomAuthService dexcomAuthService;
	private final DexcomConfig dexcomConfig;
	private final GlucoseService glucoseService;

	@PostConstruct
	public void init() {
		log.info("DexcomController 초기화 완료");
	}

	/*
	 * Auth 페이지로 리다이렉팅
	 * */
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

	/*
	 * Code -> Token 교환
	 * */
	@GetMapping("/code")
	public ResponseEntity<String> handleOAuthCallback(@RequestParam Map<String, String> params) {
		if (params.containsKey("error")) {
			log.error("OAuth 인증 에러: {}", params.get("error"));
			return ResponseEntity.badRequest().body("인증 실패: " + params.get("error"));
		}
		// 추후 수정
		Long userId = 1L;

		return ResponseEntity.ok().body(dexcomAuthService.exchangeCodeToToken(params, userId));
	}

	/*
	 * accessToken 갱신
	 * */
	@PostMapping("/refresh")
	public ResponseEntity<String> refreshAccessToken() {
		// 추후 수정
		Long userId = 1L;

		return ResponseEntity.ok().body(dexcomAuthService.refreshAccessToken(userId));
	}

	/*
	 * 혈당 데이터
	 * */
	@GetMapping("/{dexcomId}/egvs")
	public ResponseEntity<String> getEgvsFromDexcom(
		@PathVariable Long dexcomId
	) {
		return ResponseEntity.ok().body(glucoseService.saveEgvs(dexcomId));
	}

	@GetMapping("/{dexcomId}/egvs/period")
	public ResponseEntity<String> saveEgvsWithPeriod(
		@PathVariable Long dexcomId,
		@RequestParam String startDate,
		@RequestParam String endDate
	) {
		String result = glucoseService.saveEgvsWithPeriod(dexcomId, startDate, endDate);
		return ResponseEntity.ok(result);
	}

	/*
	 * 기기정보
	 * */
	@GetMapping("/device")
	public ResponseEntity<String> getDeviceInfoFromDexcom() {
		// 수정
		Long userId = 1L;

		return ResponseEntity.ok().body(dexcomService.updateDeviceInfo(userId));
	}

	/*
	 * 내 덱스콤 설정 조회
	 */
	@GetMapping("/my/setting")
	public ResponseEntity<DexcomDto> getDexcomInfo(){
		Long userId = 1L;

		return dexcomService.getDexcomInfo(userId);
	}

	/*
	 * max_glucose, min_glucose 수정하는 로직 추가
	 * */
	@PutMapping("/setting")
	public ResponseEntity<String> setMinMaxGlucose(
		@RequestParam Integer min,
		@RequestParam Integer max) {
		// 수정
		Long userId = 1L;
		return ResponseEntity.ok().body(dexcomService.updateMinMaxGlucose(userId, min, max));
	}

	/*
	 * 특정 기간 혈당 데이터 리스트
	 * */
	@GetMapping("/my/{dexcomId}/egvs")
	public List<GlucoseDto> getMyGlucoseList(
		@PathVariable Long dexcomId,
		@RequestParam(required = false) String startDate,
		@RequestParam(required = false) String endDate
	) {
		return glucoseService.getMyEgvs(dexcomId, startDate, endDate);
	}

	/*
	 * 전체 기간 혈당 데이터 리스트
	 * */
	@GetMapping("/my/all/{dexcomId}/egvs")
	public List<GlucoseDto> getMyAllGlucoseList(
		@PathVariable Long dexcomId
	) {
		return glucoseService.getMyAllEgvs(dexcomId);
	}
}
