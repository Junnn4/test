package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "cgm")
public class CGM {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "dexcom_id", nullable = false)
	private Long dexcomId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "is_connected", length = 20)
	private String isConnected;

	@Column(name = "max_glucose", nullable = false)
	private Integer maxGlucose;

	@Column(name = "min_glucose")
	private Integer minGlucose;

	@Column(name = "last_egv_time")
	private LocalDateTime lastEgvTime;

	@Column(name = "access_token", columnDefinition = "TEXT")
	private String accessToken;

	@Column(name = "refresh_token", length = 255)
	private String refreshToken;

	@Column(name = "issued_at")
	private LocalDateTime issuedAt; // 리프레시 토큰 발급 시간

	@Column(name = "expires_in")
	private LocalDateTime expiresIn; // 엑세스 토큰 만료 시간
}
