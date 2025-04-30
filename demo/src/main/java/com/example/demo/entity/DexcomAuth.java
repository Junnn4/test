package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
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
@Table(name = "dexcom_auth")
public class DexcomAuth {

	@Id
	@Column(name = "dexcom_id", nullable = false)
	private Long dexcomId;

	@OneToOne
	@JoinColumn(name = "dexcom_id", referencedColumnName = "dexcom_id", insertable = false, updatable = false)
	private Dexcom dexcom; // FK

	@Column(name = "access_token", columnDefinition = "TEXT")
	private String accessToken;

	@Column(name = "refresh_token", length = 255)
	private String refreshToken;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt; // 리프레시 토큰 발급 시간

	@Column(name = "expires_in")
	private LocalDateTime expiresIn; // 엑세스 토큰 만료 시간


	public void updateAccessToken(String accessToken) {
		this.accessToken = accessToken;
		this.expiresIn = LocalDateTime.now().plusDays(2);
	}

}
