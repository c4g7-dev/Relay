package com.c4g7.relay.util;

import net.minecraft.locale.Language;

/** Translation lookup with an English fallback, so a missing language key never shows up raw. */
public final class Tr {
	private Tr() {
	}

	public static String get(String key, String english) {
		try {
			Language language = Language.getInstance();
			return language.has(key) ? language.getOrDefault(key) : english;
		} catch (RuntimeException | LinkageError notReady) {
			return english;
		}
	}
}
