package com.example.demo.test;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import com.example.demo.service.DexcomAuthService;
import com.example.demo.service.DexcomService;
import com.example.demo.service.GlucoseService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/glucose")
@CrossOrigin(origins = "*")
public class DexcomController {
	private static final Logger log = LoggerFactory.getLogger(DexcomController.class);
	private final DexcomService dexcomService;

	@Value("${dexcom.client-id}")
	private String clientId;
	@Value("${dexcom.client-secret}")
	private String clientSecret;
	@Value("${dexcom.redirect-uri}")
	private String redirectUri;

	private static final String AUTHORIZATION_ENDPOINT = "https://api.dexcom.eu/v2/oauth2/login";
	private static final String TOKEN_ENDPOINT         = "https://api.dexcom.eu/v2/oauth2/token";
	private static final String EGV_ENDPOINT           = "https://api.dexcom.eu/v3/users/self/egvs";
	private static final String DEVICE_ENDPOINT        = "https://api.dexcom.eu/v3/users/self/devices";

	// 샌드박스용
	// private static final String AUTHORIZATION_ENDPOINT = "https://sandbox-api.dexcom.com/v2/oauth2/login";
	// private static final String TOKEN_ENDPOINT         = "https://sandbox-api.dexcom.com/v2/oauth2/token";
	// private static final String EGV_ENDPOINT           = "https://sandbox-api.dexcom.com/v3/users/self/egvs";
	// private static final String DEVICE_ENDPOINT        = "https://sandbox-api.dexcom.com/v3/users/self/devices";

	private final DexcomAuthService dexcomAuthService;
	private final GlucoseService glucoseService;

	private final RestTemplate restTemplate = new RestTemplate();

	private final DateTimeFormatter isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	@PostConstruct
	public void init() {
		log.info("DexcomController 초기화 완료");
	}

	/** OAuth 로그인 페이지로 리디렉트 */
	@GetMapping("/auth")
	public void authorize(HttpServletResponse response) throws IOException {
		String state = UUID.randomUUID().toString(); // CSRF 방지용 랜덤 state

		String url = UriComponentsBuilder
			.fromHttpUrl(AUTHORIZATION_ENDPOINT)
			.queryParam("client_id", clientId)
			.queryParam("redirect_uri", redirectUri)
			.queryParam("response_type", "code")
			.queryParam("scope", "offline_access")
			.queryParam("state", state)
			.toUriString();

		log.info("Redirecting to Dexcom OAuth: {}", url);
		response.sendRedirect(url);
	}

	/** 콜백에서 code → token 교환 **/
	@GetMapping("/code")
	public ResponseEntity<String> handleOAuthCallback(@RequestParam Map<String, String> params) {
		if (params.containsKey("error")) {
			log.error("OAuth 인증 에러: {}", params.get("error"));
			return ResponseEntity.badRequest().body("인증 실패: " + params.get("error"));
		}

		String code = params.get("code");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("client_id", clientId);
		body.add("client_secret", clientSecret);
		body.add("code", code);
		body.add("grant_type", "authorization_code");
		body.add("redirect_uri", redirectUri);

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
		ResponseEntity<Map> resp = restTemplate.postForEntity(TOKEN_ENDPOINT, request, Map.class);

		if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
			String accessToken  = (String) resp.getBody().get("access_token");
			String refreshToken = (String) resp.getBody().get("refresh_token");
			// 추후수정
			Long userId = 1L;

			// 2. (샘플) 기기 정보 요청
			HttpHeaders deviceHeaders = new HttpHeaders();
			deviceHeaders.setBearerAuth(accessToken);
			HttpEntity<Void> deviceRequest = new HttpEntity<>(deviceHeaders);

			ResponseEntity<String> deviceResp = restTemplate.exchange(
				DEVICE_ENDPOINT,
				HttpMethod.GET,
				deviceRequest,
				String.class
			);

			dexcomService.saveDexcomSettingInfo(userId, deviceResp.getBody());

			dexcomAuthService.saveToken(userId, accessToken, refreshToken);

			log.info("토큰 발급 성공");
			return ResponseEntity.ok("토큰 발급 성공");
		} else {
			log.error("토큰 요청 실패: {}", resp.getStatusCode());
			return ResponseEntity.status(resp.getStatusCode()).body("토큰 요청 실패");
		}
	}

	/** refresh_token으로 access_token 갱신 */
	@PostMapping("/refresh")
	public ResponseEntity<String> refreshAccessToken() {
		// 추후 수정
		Long userId = 1L;

		String refreshToken = dexcomAuthService.getRefreshTokenByUserId(userId);

		if (refreshToken == null) {
			log.warn("갱신 시도했으나 refresh_token 없음");
			return ResponseEntity.badRequest().body("refresh_token 없음. 먼저 인증하세요.");
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("client_id", clientId);
		body.add("client_secret", clientSecret);
		body.add("grant_type", "refresh_token");
		body.add("refresh_token", refreshToken);

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
		ResponseEntity<Map> resp = restTemplate.postForEntity(TOKEN_ENDPOINT, request, Map.class);

		if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
			String newAccessToken = (String) resp.getBody().get("access_token");
			dexcomAuthService.updateAccessTokenByRefreshToken(userId, newAccessToken);
			log.info("🔁 access_token 갱신 성공");
			return ResponseEntity.ok("access_token 갱신 완료");
		} else {
			log.error("토큰 갱신 실패: {}", resp.getStatusCode());
			return ResponseEntity.status(resp.getStatusCode()).body("갱신 실패");
		}
	}


	/** access_token으로 혈당 데이터 조회 */
	@GetMapping("/{dexcomId}/egvs")
	public ResponseEntity<String> getEgvs(
		@PathVariable Long dexcomId,
		@RequestParam(required = false) String startDate,
		@RequestParam(required = false) String endDate
	) {
		// 수정
		Long userId = 1L;

		String accessToken = dexcomAuthService.getAccessTokenByUserId(userId);

		if (accessToken == null) {
			log.warn("access_token 없음. 먼저 인증하세요.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("access_token 없음. 먼저 인증하세요.");
		}

		LocalDateTime end = (endDate != null)
			? LocalDateTime.parse(endDate, isoFormatter)
			: LocalDateTime.now();
		LocalDateTime start = (startDate != null)
			? LocalDateTime.parse(startDate, isoFormatter)
			: end.minusHours(1);

		String url = EGV_ENDPOINT + "?startDate=2023-01-01T00:00:00&endDate=2023-01-01T01:00:00";
			// 수정
			// "?startDate=" + isoFormatter.format(start) +
			// "&endDate="   + isoFormatter.format(end);

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<Void> request = new HttpEntity<>(headers);
		ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

		glucoseService.saveGlucoseFromDexcom(resp.getBody(), dexcomId);

		log.info("📥 EGVS 호출: start={} end={} → HTTP {}", start, end, resp.getStatusCode());
		return resp;
	}

	/*
	* max_glucose, min_glucose 수정하는 로직 추가
	* */

	/*
	 * 기기정보 조회하는 로직 추가
	 * */

	/*
	 * 혈당스파이크 확인하는 로직 추가
	 * */

}
