package com.example.demo.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Component
@Getter
public class DexcomConfig {

	@Value("${dexcom.client-id}")
	private String clientId;

	@Value("${dexcom.client-secret}")
	private String clientSecret;

	@Value("${dexcom.redirect-uri}")
	private String redirectUri;

//	private String AUTHORIZATION_ENDPOINT = "https://api.dexcom.eu/v2/oauth2/login";
//
//	private String TOKEN_ENDPOINT         = "https://api.dexcom.eu/v2/oauth2/token";
//
//	private String EGV_ENDPOINT           = "https://api.dexcom.eu/v3/users/self/egvs";
//
//	private String DEVICE_ENDPOINT        = "https://api.dexcom.eu/v3/users/self/devices";

	// 샌드박스용
	 private String AUTHORIZATION_ENDPOINT = "https://sandbox-api.dexcom.com/v2/oauth2/login";
	 private String TOKEN_ENDPOINT         = "https://sandbox-api.dexcom.com/v2/oauth2/token";
	 private String EGV_ENDPOINT           = "https://sandbox-api.dexcom.com/v3/users/self/egvs";
	 private String DEVICE_ENDPOINT        = "https://sandbox-api.dexcom.com/v3/users/self/devices";
}

