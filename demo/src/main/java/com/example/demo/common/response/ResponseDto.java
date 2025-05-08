package com.example.demo.common.response;

import java.io.Serializable;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResponseDto<T> implements Serializable {
	private T data;

	public static <T> ResponseEntity<ResponseDto<T>> of(HttpStatus status, T data) {
		return ResponseEntity.status(status).body(new ResponseDto<T>(data));
	}

	public static <T> ResponseEntity<ResponseDto<T>> ok(T data) {

		return ResponseEntity.ok(new ResponseDto<T>(data));
	}

	public static <T> ResponseEntity<ResponseDto<T>> created(T data) {

		return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseDto<T>(data));
	}

	public static ResponseEntity<Void> noContent() {

		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}
}
