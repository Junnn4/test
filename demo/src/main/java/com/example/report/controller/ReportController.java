package com.example.report.controller;

import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/report")
@CrossOrigin(origins = "*")
public class ReportController {

	/*
	 * 실시간 혈당그래프 조회
	 * */


	/*
	 * 혈당스파이크 정보 조회
	 * */


	/*
	 * 식사 시간의 20분 전, 2시간 후 혈당 조회
	 * */
	@GetMapping("/glucose")
	public String getUserGlucose(
		@RequestParam LocalDateTime date,
		@RequestHeader("userId") int userId){

		return "";
	}
}