package com.example.demo.common.util;

import java.util.Objects;

import com.example.demo.common.error.GlobalErrorCodes;
import com.example.demo.common.exception.BusinessException;

public class HeaderUtil {
	public static void validateUserId(Long pathId, Long headerId) {
		if(!Objects.equals(pathId, headerId)) {
			throw new BusinessException(GlobalErrorCodes.INVALID_USER_HEADER_ID);
		}
	}
}
