package com.example.demo.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.BatchSize;

import com.example.demo.common.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@BatchSize(size = 100)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "glucose", uniqueConstraints = @UniqueConstraint(columnNames = {"dexcom_id", "recorded_at"}))
public class Glucose extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "glucose_value_id", nullable = false)
	private Long glucoseValueId;

	@ManyToOne
	@JoinColumn(name = "dexcom_id", nullable = false)
	private CGM cgm;

	@Column(name = "value", nullable = false)
	private Integer value;

	@Column(name = "transmitter_generation", length = 20)
	private String transmitterGeneration;

	@Column(name = "trend", length = 20)
	private String trend;

	@Column(name = "recorded_at", nullable = false)
	private LocalDateTime recordedAt;

	public Glucose(CGM cgm, Integer value, String transmitterGeneration, String trend, LocalDateTime recordedAt) {
		this.cgm = cgm;
		this.value = value;
		this.transmitterGeneration = transmitterGeneration;
		this.trend = trend;
		this.recordedAt = recordedAt;
	}
}
