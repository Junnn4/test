package com.example.report.common.exception;

import com.example.report.common.error.ErrorCode;

public class TemplateException extends BusinessException {
	public TemplateException(ErrorCode errorCode) {
		super(errorCode);
	}
}