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
import com.example.demo.common.error.GlobalErrorCodes;
import com.example.demo.common.exception.BusinessException;
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
		body.add("client_id",     dexcomConfig.getClientId());
		body.add("client_secret", dexcomConfig.getClientSecret());
		body.add("code",          code);
		body.add("grant_type",    "authorization_code");
		body.add("redirect_uri",  dexcomConfig.getRedirectUri());

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
		ResponseEntity<Map> resp;
		try {
			resp = restTemplate.postForEntity(dexcomConfig.getTOKEN_ENDPOINT(), request, Map.class);
		} catch (Exception e) {
			log.error("exchangeCodeToToken: 토큰 요청 중 오류 발생", e);
			throw new BusinessException(GlobalErrorCodes.DEXCOM_TOKEN_REQUEST_FAILED);
		}

		if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
			log.error("exchangeCodeToToken: 토큰 요청 실패 status={}", resp.getStatusCode());
			throw new BusinessException(GlobalErrorCodes.DEXCOM_TOKEN_REQUEST_FAILED);
		}

		String accessToken  = (String) resp.getBody().get("access_token");
		String refreshToken = (String) resp.getBody().get("refresh_token");

		// 기기 정보 조회
		HttpHeaders deviceHeaders = new HttpHeaders();
		deviceHeaders.setBearerAuth(accessToken);
		HttpEntity<Void> deviceRequest = new HttpEntity<>(deviceHeaders);

		String deviceBody;
		try {
			ResponseEntity<String> deviceResp = restTemplate.exchange(
				dexcomConfig.getDEVICE_ENDPOINT(),
				HttpMethod.GET,
				deviceRequest,
				String.class
			);
			if (!deviceResp.getStatusCode().is2xxSuccessful() || deviceResp.getBody() == null) {
				log.error("exchangeCodeToToken: 기기 정보 조회 실패 status={}", deviceResp.getStatusCode());
				throw new BusinessException(GlobalErrorCodes.DEXCOM_DEVICE_INFO_FETCH_FAILED);
			}
			deviceBody = deviceResp.getBody();
		} catch (BusinessException be) {
			throw be;
		} catch (Exception e) {
			log.error("exchangeCodeToToken: 기기 정보 조회 중 오류 발생", e);
			throw new BusinessException(GlobalErrorCodes.DEXCOM_DEVICE_INFO_FETCH_FAILED);
		}

		// 저장 로직
		Dexcom dexcom = dexcomService.saveDexcomSettingInfo(userId, deviceBody);
		DexcomAuth dexcomAuth = DexcomConverter.create(
			dexcom.getDexcomId(),
			accessToken,
			refreshToken,
			LocalDateTime.now(),
			LocalDateTime.now().plusHours(2)
		);
		dexcomAuthRepository.save(dexcomAuth);

		log.info("exchangeCodeToToken: 토큰 발급 및 저장 완료 (dexcomId={})", dexcom.getDexcomId());
		return "Successfully issued token";
	}

	@Transactional
	public String refreshAccessToken(Long dexcomId) {
		DexcomAuth auth = dexcomAuthRepository.findByDexcomId(dexcomId)
			.orElseThrow(() -> {
				log.error("refreshAccessToken: DexcomAuth 정보 없음 (dexcomId={})", dexcomId);
				return new BusinessException(GlobalErrorCodes.DEXCOM_AUTH_NOT_FOUND);
			});

		if (auth.getRefreshToken() == null) {
			log.warn("refreshAccessToken: refresh_token 없음 (dexcomId={})", dexcomId);
			throw new BusinessException(GlobalErrorCodes.DEXCOM_NO_REFRESH_TOKEN);
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("client_id",     dexcomConfig.getClientId());
		body.add("client_secret", dexcomConfig.getClientSecret());
		body.add("grant_type",    "refresh_token");
		body.add("refresh_token", auth.getRefreshToken());

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
		ResponseEntity<Map> resp;
		try {
			resp = restTemplate.postForEntity(dexcomConfig.getTOKEN_ENDPOINT(), request, Map.class);
		} catch (Exception e) {
			log.error("refreshAccessToken: 토큰 갱신 중 오류 발생", e);
			throw new BusinessException(GlobalErrorCodes.DEXCOM_TOKEN_REFRESH_FAILED);
		}

		if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
			log.error("refreshAccessToken: 토큰 갱신 실패 status={}", resp.getStatusCode());
			throw new BusinessException(GlobalErrorCodes.DEXCOM_TOKEN_REFRESH_FAILED);
		}

		String newAccessToken = (String) resp.getBody().get("access_token");
		String newRefreshToken = (String) resp.getBody().get("refresh_token");
		LocalDateTime issuedAt = LocalDateTime.now();
		LocalDateTime expiresAt = issuedAt.plusHours(2);

		dexcomAuthRepository.updateTokens(dexcomId, newAccessToken, newRefreshToken, issuedAt, expiresAt) ;

		log.info("토큰 갱신 완료 (dexcomId={})", dexcomId);
		return "토큰 갱신 완료";
	}
}