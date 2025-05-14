package com.example.report.controller;

import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.report.dto.ReportDto;

@RestController("/api/v1/report")
public class ReportController {

	/*
	* 특정 날짜 리포트 조회
	* */
	@GetMapping("/my")
	public ResponseEntity<ReportDto> getReport(
		@RequestParam LocalDateTime date) {



		return ResponseEntity.ok().body(new ReportDto());
	}

	/*
	* meal에 있는 식사 정보 조회
	* */
	@GetMapping("/food")
	public ResponseEntity<String> getFoodInfo(){


		return ResponseEntity.ok().body("");
	}

	@PostMapping("/nutrition")
	public void setMyNutrition(
		@RequestBody ){

	}





}
