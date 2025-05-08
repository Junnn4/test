package com.example.demo.entity;
import java.time.LocalDateTime;

import com.example.demo.common.entity.BaseEntity;

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
@Table(name = "dexcom")
public class Dexcom extends BaseEntity {

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
}
