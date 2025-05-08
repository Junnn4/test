package com.example.demo.common.error;

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

	/** =======================
	 *  덱스콤 에러 (DEXCOM)
	 *  ======================= */

	DEXCOM_JSON_PARSE_ERROR(        HttpStatus.BAD_REQUEST,"DEXCOM001","Dexcom JSON 파싱 오류"),
	DEXCOM_RECORDS_MISSING(         HttpStatus.BAD_REQUEST,"DEXCOM002","Dexcom 응답에 records 필드 누락"),
	DEXCOM_RECORDS_EMPTY(           HttpStatus.BAD_REQUEST,"DEXCOM003", "Dexcom records 배열이 비어 있음"),
	DEXCOM_MISSING_LAST_UPLOAD(     HttpStatus.BAD_REQUEST,"DEXCOM004","lastUploadDate 필드 누락"),
	DEXCOM_LAST_UPLOAD_PARSE_ERROR( HttpStatus.BAD_REQUEST,"DEXCOM005","lastUploadDate 파싱 실패"),
	DEXCOM_NOT_FOUND(               HttpStatus.NOT_FOUND,"DEXCOM100","Dexcom 정보 없음"),
	DEXCOM_AUTH_NOT_FOUND(          HttpStatus.NOT_FOUND, "DEXCOM101","Dexcom Auth 정보 없음"),
	DEXCOM_NO_ACCESS_TOKEN(         HttpStatus.UNAUTHORIZED,"DEXCOM102","Access Token 없음"),
	DEXCOM_DEVICE_RECORDS_INVALID(  HttpStatus.BAD_REQUEST,"DEXCOM103","기기 조회 records 형식 오류"),
	DEXCOM_DEVICE_DATE_PARSE_ERROR( HttpStatus.BAD_REQUEST,"DEXCOM104","기기 조회 lastUploadDate 파싱 실패"),
	DEXCOM_DEVICE_UPDATE_FAILED(    HttpStatus.INTERNAL_SERVER_ERROR,"DEXCOM105","기기 정보 업데이트 실패"),
	DEXCOM_INVALID_MIN_MAX(         HttpStatus.BAD_REQUEST,"DEXCOM106","잘못된 min/max 입력"),
	DEXCOM_TOKEN_REQUEST_FAILED(    HttpStatus.BAD_GATEWAY,      "DEXCOM107", "토큰 요청 실패"),
	DEXCOM_DEVICE_INFO_FETCH_FAILED(HttpStatus.BAD_GATEWAY,      "DEXCOM108", "기기 정보 조회 실패"),
	DEXCOM_NO_REFRESH_TOKEN(        HttpStatus.BAD_REQUEST,      "DEXCOM109", "refresh_token 없음"),
	DEXCOM_TOKEN_REFRESH_FAILED(    HttpStatus.BAD_GATEWAY,      "DEXCOM110", "토큰 갱신 실패"),
	DEXCOM201_NO_AUTH(              HttpStatus.UNAUTHORIZED,    "DEXCOM201", "Dexcom Auth 정보 없음"),
	DEXCOM202_INVALID_TOKEN(        HttpStatus.BAD_REQUEST,      "DEXCOM202", "access_token 없음"),
	DEXCOM203_EGV_FETCH_FAILED(     HttpStatus.BAD_GATEWAY,      "DEXCOM203", "혈당 데이터 조회 실패"),
	DEXCOM204_JSON_PARSE_FAILED(    HttpStatus.INTERNAL_SERVER_ERROR,"DEXCOM204", "혈당 데이터 파싱 실패"),
	DEXCOM205_INVALID_PERIOD(       HttpStatus.BAD_REQUEST,         "DEXCOM205", "잘못된 기간 파라미터"),


	INVALID_USER_HEADER_ID(HttpStatus.BAD_REQUEST, "MEMBER4032", "잘못된 유저 ID 입니다."),

	;

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;
}
