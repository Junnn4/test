package com.example.demo.entity;

public enum GlucoseLevel {
	VERY_LOW("매우 낮음", 0, 54),           // 0 ~ 53
	LOW("낮음", 54, 70),                  // 54 ~ 69
	NORMAL("보통", 70, 181),             // 70 ~ 180
	HIGH("높음", 181, 251),              // 181 ~ 250
	VERY_HIGH("매우 높음", 251, Integer.MAX_VALUE); // 251 ~

	private final String label;
	private final int min;
	private final int max;

	GlucoseLevel(String label, int min, int max) {
		this.label = label;
		this.min = min;
		this.max = max;
	}

	public static String getLevel(int value) {
		for (GlucoseLevel level : values()) {
			if (value >= level.min && value < level.max) {
				return level.label;
			}
		}
		return "알 수 없음";
	}
}
