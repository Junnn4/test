package com.example.report.common.error;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GlobalErrorCodes implements ErrorCode {

	/** =======================
	 *  공통 에러 (COMMON)
	 *  ======================= */
	INTERNAL_SERVER_ERROR(HttpStatus.BAD_REQUEST, "COMMON4001", "내부 서버 오류"),
	NOT_FOUND_URL(HttpStatus.NOT_FOUND, "COMMON4002", "존재하지 않는 URL"),
	INVALID_JSON_DATA(HttpStatus.BAD_REQUEST, "COMMON4003", "잘못된 형식의 JSON 데이터"),
	INVALID_HEADER_DATA(HttpStatus.BAD_REQUEST, "COMMON4004", "잘못된 형식의 헤더 데이터"),
	INVALID_USER_ID(HttpStatus.BAD_REQUEST, "COMMON4005", "잘못된 userId 형식"),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON4011", "로그인이 필요합니다."),

	INVALID_USER_HEADER_ID(HttpStatus.BAD_REQUEST, "MEMBER4032", "잘못된 유저 ID 입니다."),

	;

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;
}
