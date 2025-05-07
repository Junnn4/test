package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.example.demo.common.DexcomConfig;
import com.example.demo.convert.DexcomConverter;
import com.example.demo.entity.Dexcom;
import com.example.demo.entity.DexcomAuth;
import com.example.demo.repository.DexcomAuthRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DexcomAuthService {

	private final DexcomConfig dexcomConfig;
	private final DexcomService dexcomService;
	private final DexcomAuthRepository dexcomAuthRepository;

	private final RestTemplate restTemplate = new RestTemplate();

	@Transactional
	public String exchangeCodeToToken(Map<String, String> params, Long userId) {
		String code = params.get("code");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("client_id", dexcomConfig.getClientId());
		body.add("client_secret", dexcomConfig.getClientSecret());
		body.add("code", code);
		body.add("grant_type", "authorization_code");
		body.add("redirect_uri", dexcomConfig.getRedirectUri());

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
		ResponseEntity<Map> resp = restTemplate.postForEntity(dexcomConfig.getTOKEN_ENDPOINT(), request, Map.class);

		if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
			String accessToken  = (String) resp.getBody().get("access_token");
			String refreshToken = (String) resp.getBody().get("refresh_token");

			HttpHeaders deviceHeaders = new HttpHeaders();
			deviceHeaders.setBearerAuth(accessToken);
			HttpEntity<Void> deviceRequest = new HttpEntity<>(deviceHeaders);

			ResponseEntity<String> deviceResp = restTemplate.exchange(
				dexcomConfig.getDEVICE_ENDPOINT(),
				HttpMethod.GET,
				deviceRequest,
				String.class
			);

			Dexcom dexcom = dexcomService.saveDexcomSettingInfo(userId, deviceResp.getBody());

			DexcomAuth dexcomAuth = DexcomConverter.create(dexcom.getDexcomId(), accessToken, refreshToken, LocalDateTime.now(), LocalDateTime.now().plusHours(2));
			dexcomAuthRepository.save(dexcomAuth);

			log.info("토큰 발급 성공");
			return "Successfully issued token";
		} else {
			log.error("토큰 요청 실패: {}", resp.getStatusCode());
			return "Failed to issue token";
		}
	}

	public String refreshAccessToken(Long userId) {
		DexcomAuth auth = dexcomAuthRepository.findByDexcom_UserId(userId)
			.orElseThrow(() -> new RuntimeException("Dexcom Auth 정보 없음"));

		if (auth.getRefreshToken() == null) {
			log.warn("갱신 시도했으나 refresh_token 없음");
			return "No exist refresh_token";
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("client_id", dexcomConfig.getClientId());
		body.add("client_secret", dexcomConfig.getClientSecret());
		body.add("grant_type", "refresh_token");
		body.add("refresh_token", auth.getRefreshToken());

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
		ResponseEntity<Map> resp = restTemplate.postForEntity(dexcomConfig.getTOKEN_ENDPOINT(), request, Map.class);

		if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
			String newAccessToken = (String) resp.getBody().get("access_token");

			auth.updateAccessToken(newAccessToken);
			dexcomAuthRepository.save(auth);

			log.info("🔁 access_token 갱신 성공");
			return "access_token 갱신 완료";
		} else {
			log.error("토큰 갱신 실패: {}", resp.getStatusCode());
			return "갱신 실패";
		}
	}
}
