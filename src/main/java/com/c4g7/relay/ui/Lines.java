package com.c4g7.relay.ui;

import com.c4g7.relay.chat.ChatTab;
import com.c4g7.relay.chat.Entry;
import com.c4g7.relay.config.Style;
import com.c4g7.relay.util.Rect;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

/**
 * Turns a tab's entries into wrapped screen lines. Wrapping is cached per entry and only redone
 * when the width or the timestamp style changes, instead of re-splitting every message every frame.
 */
public final class Lines {
	public record Line(FormattedCharSequence text, float alpha, Entry entry, boolean first) {
	}

	private static final Map<String, DateTimeFormatter> FORMATTERS = new ConcurrentHashMap<>();
	private static final DateTimeFormatter FALLBACK = DateTimeFormatter.ofPattern(Style.DEFAULT_TIME_FORMAT, Locale.ROOT);

	private Lines() {
	}

	/** All visible lines of {@code tab}, oldest first; faded-out lines are skipped unless {@code everything}. */
	public static List<Line> of(ChatTab tab, net.minecraft.client.gui.Font font, int width, Style style, boolean everything) {
		List<Line> lines = new ArrayList<>();
		if (tab == null || width <= 0) {
			return lines;
		}
		long now = System.currentTimeMillis();
		int key = Objects.hash(width, style.timestamps, style.timestamps ? style.timestampFormat : "", style.timestampColor);
		for (Entry entry : tab.entries()) {
			float alpha = everything ? 1.0F : fade(now - entry.message().time());
			if (alpha <= 0.0F) {
				continue;
			}
			List<FormattedCharSequence> wrapped = entry.cachedLines(key);
			if (wrapped == null) {
				wrapped = entry.message().isDivider() ? List.of(entry.message().content().getVisualOrderText()) : font.split(display(entry, style), width);
				entry.cacheLines(key, wrapped);
			}
			for (int index = 0; index < wrapped.size(); index++) {
				lines.add(new Line(wrapped.get(index), alpha, entry, index == 0));
			}
		}
		return lines;
	}

	/** Lines of the body, bottom-aligned and scrolled: {@code [first, last)} into {@code lines}. */
	public static int[] window(Rect body, int count, int scroll) {
		int rows = Math.max(0, body.height() / Theme.LINE_HEIGHT);
		int first = Math.max(0, count - rows - scroll);
		int last = Math.min(count, first + rows);
		return new int[] {first, last};
	}

	/** Index of the line under {@code y}, or -1. */
	public static int at(Rect body, int count, int scroll, double y) {
		if (count <= 0 || y < body.y() || y >= body.bottom()) {
			return -1;
		}
		int fromBottom = (int) ((body.bottom() - 1 - y) / Theme.LINE_HEIGHT);
		int[] range = window(body, count, scroll);
		int index = range[1] - 1 - fromBottom;
		return index >= range[0] && index < range[1] ? index : -1;
	}

	public static Component display(Entry entry, Style style) {
		Component content = entry.message().content();
		if (!style.timestamps && entry.repeats() <= 1) {
			return content;
		}
		MutableComponent line = Component.empty();
		if (style.timestamps) {
			String time = time(entry.message().time(), style.timestampFormat);
			line.append(Component.literal("[" + time + "] ").withStyle(text -> text.withColor(style.timestampColor & 0xFFFFFF)));
		}
		line.append(content);
		if (entry.repeats() > 1) {
			line.append(Component.literal(" (" + entry.repeats() + ")").withStyle(ChatFormatting.GRAY));
		}
		return line;
	}

	public static String copyText(Entry entry, Style style) {
		String text = entry.message().copyText();
		if (style.timestamps && !text.isEmpty() && !entry.message().isDivider()) {
			return "[" + time(entry.message().time(), style.timestampFormat) + "] " + text;
		}
		return text;
	}

	public static String time(long epochMillis, String pattern) {
		DateTimeFormatter formatter = FORMATTERS.computeIfAbsent(pattern == null ? "" : pattern, key -> {
			try {
				return key.isBlank() ? FALLBACK : DateTimeFormatter.ofPattern(key, Locale.ROOT);
			} catch (IllegalArgumentException invalid) {
				return FALLBACK;
			}
		});
		try {
			return formatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()));
		} catch (RuntimeException unsupported) {
			return FALLBACK.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()));
		}
	}

	/** Full for ten seconds, then fades out over one, like vanilla chat. */
	public static float fade(long age) {
		if (age < Theme.VISIBLE) {
			return 1.0F;
		}
		return age > Theme.VISIBLE + Theme.FADE ? 0.0F : 1.0F - (age - Theme.VISIBLE) / (float) Theme.FADE;
	}
}
