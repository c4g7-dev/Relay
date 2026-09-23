package com.c4g7.relay.config;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

/** Writes ARGB colours as {@code "#AARRGGBB"} so the config stays readable; also accepts plain numbers. */
public final class HexColor extends TypeAdapter<Integer> {
	@Override
	public void write(JsonWriter out, Integer value) throws IOException {
		out.value(value == null ? null : String.format("#%08X", value));
	}

	@Override
	public Integer read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NUMBER) {
			return (int) in.nextLong();
		}
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return 0;
		}
		return parse(in.nextString(), 0);
	}

	/** Parses {@code #RGB}, {@code #RRGGBB} or {@code #AARRGGBB} (the # is optional); six digits mean opaque. */
	public static int parse(String text, int fallback) {
		if (text == null) {
			return fallback;
		}
		String digits = text.trim();
		if (digits.startsWith("#")) {
			digits = digits.substring(1);
		} else if (digits.startsWith("0x") || digits.startsWith("0X")) {
			digits = digits.substring(2);
		}
		try {
			return switch (digits.length()) {
				case 3 -> {
					int r = Character.digit(digits.charAt(0), 16);
					int g = Character.digit(digits.charAt(1), 16);
					int b = Character.digit(digits.charAt(2), 16);
					if (r < 0 || g < 0 || b < 0) {
						yield fallback;
					}
					yield 0xFF000000 | r * 0x110000 | g * 0x1100 | b * 0x11;
				}
				case 6 -> 0xFF000000 | Integer.parseInt(digits, 16);
				case 8 -> (int) Long.parseLong(digits, 16);
				default -> fallback;
			};
		} catch (NumberFormatException invalid) {
			return fallback;
		}
	}
}
