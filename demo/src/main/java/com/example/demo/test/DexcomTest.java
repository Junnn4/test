package com.example.demo.test;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@CrossOrigin(origins = "*")
public class DexcomTest {

	// ✅ SANDBOX 환경: Access Token 발급
	@GetMapping("/callback")
	public String handleOAuthCallback(@RequestParam Map<String, String> params) {
		if (params.containsKey("error")) {
			System.out.println("인증 실패: " + params.get("error"));
			return "인증 실패!";
		}

		String authCode = params.get("code");
		System.out.println("인증 성공, code: " + authCode);

		String tokenEndpoint = "https://sandbox-api.dexcom.com/v2/oauth2/token"; // 샌드박스 URL
		// https://api.dexcom.com/v2/oauth2/token

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("client_id", "ZJUBNkLmsJFrEVLjGuVTdYaLivhDelux");
		body.add("client_secret", "xtYyDZY47Dpl3zTt");
		body.add("code", authCode);
		body.add("grant_type", "authorization_code");
		body.add("redirect_uri", "http://localhost:8080/callback");

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> response = restTemplate.postForEntity(tokenEndpoint, request, String.class);

		System.out.println("Access Token Response: " + response.getBody());

		return "토큰 응답: " + response.getBody();
	}

	// ✅ SANDBOX 환경: 수동 토큰 요청용 (선택)
	@PostMapping("/token")
	public ResponseEntity<String> getAccessToken(@RequestParam("code") String code) {
		String tokenEndpoint = "https://sandbox-api.dexcom.com/v2/oauth2/token"; // 샌드박스 URL

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("client_id", "ZJUBNkLmsJFrEVLjGuVTdYaLivhDelux");
		body.add("client_secret", "xtYyDZY47Dpl3zTt");
		body.add("code", code);
		body.add("grant_type", "authorization_code");
		body.add("redirect_uri", "http://localhost:8080/callback");

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> response = restTemplate.postForEntity(tokenEndpoint, request, String.class);

		return response;
	}

	// ✅ SANDBOX 환경: 혈당 데이터 호출
	@GetMapping("/egvs")
	public ResponseEntity<String> getEgvs(@RequestParam("access_token") String accessToken) {
		String apiUrl = "https://sandbox-api.dexcom.com/v3/users/self/egvs" +
			"?startDate=2023-01-01T00:00:00&endDate=2023-01-01T01:00:00";
		// https://api.dexcom.com/v3/users/self/egvs

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<String> request = new HttpEntity<>(headers);

		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, request, String.class);

		System.out.println("EGV Data: " + response.getBody());
		return response;
	}
}
