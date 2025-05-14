package com.example.report.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TimeUtil {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	public static String toUTCString(String localTimeString) {
		LocalDateTime localTime = LocalDateTime.parse(localTimeString, ISO_FORMATTER);
		ZonedDateTime kstZoned = localTime.atZone(KST);
		ZonedDateTime utcZoned = kstZoned.withZoneSameInstant(ZoneOffset.UTC);
		return utcZoned.format(DateTimeFormatter.ISO_INSTANT);
	}

	public static String toKSTString(String utcTimeString) {
		Instant instant = Instant.parse(utcTimeString);
		ZonedDateTime kstTime = instant.atZone(KST);
		return kstTime.format(ISO_FORMATTER);
	}

	public static LocalDateTime toKSTLocalDateTime(String utcTimeString) {
		Instant instant = Instant.parse(utcTimeString);
		return instant.atZone(KST).toLocalDateTime();
	}

	public static LocalDateTime toUTCLocalDateTime(String localTimeString) {
		LocalDateTime localTime = LocalDateTime.parse(localTimeString, ISO_FORMATTER);
		ZonedDateTime kstZoned = localTime.atZone(KST);
		return kstZoned.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
	}
}
