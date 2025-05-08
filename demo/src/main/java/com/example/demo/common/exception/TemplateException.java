package com.example.demo.common.exception;

import com.example.demo.common.error.ErrorCode;

public class TemplateException extends BusinessException {
	public TemplateException(ErrorCode errorCode) {
		super(errorCode);
	}
}