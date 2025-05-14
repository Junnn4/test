package com.example.report.common.util;

import java.util.Objects;

import com.example.report.common.error.GlobalErrorCodes;
import com.example.report.common.exception.BusinessException;

public class HeaderUtil {
	public static void validateUserId(Long pathId, Long headerId) {
		if(!Objects.equals(pathId, headerId)) {
			throw new BusinessException(GlobalErrorCodes.INVALID_USER_HEADER_ID);
		}
	}
}
