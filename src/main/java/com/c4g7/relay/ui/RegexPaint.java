package com.c4g7.relay.ui;

import com.c4g7.relay.filter.RegexSyntax;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Colours regex source text by token kind; nested groups cycle through three bracket colours. */
public final class RegexPaint {
	private static final int LITERAL = 0xFFE8E8E8;
	private static final int ESCAPE = 0xFFF0A860;
	private static final int CLASS = 0xFF7FD7FF;
	private static final int[] GROUPS = {0xFFFFD24A, 0xFFC792EA, 0xFF82AAFF};
	private static final int FLAGS = 0xFF9AA0B4;
	private static final int QUANTIFIER = 0xFFFF7AB2;
	private static final int ANCHOR = 0xFF5AD0A0;
	private static final int ALTERNATION = 0xFFFF5370;
	private static final int DOT = 0xFFFFA36C;
	private static final int QUOTED = 0xFFB5E890;

	private RegexPaint() {
	}

	/** One ARGB colour per character of {@code pattern}. */
	public static int[] colors(String pattern) {
		int[] colors = new int[pattern.length()];
		for (RegexSyntax.Token token : RegexSyntax.tokenize(pattern)) {
			int color = switch (token.kind()) {
				case LITERAL -> LITERAL;
				case ESCAPE -> ESCAPE;
				case CLASS -> CLASS;
				case GROUP -> GROUPS[token.depth() % GROUPS.length];
				case FLAGS -> FLAGS;
				case QUANTIFIER -> QUANTIFIER;
				case ANCHOR -> ANCHOR;
				case ALTERNATION -> ALTERNATION;
				case DOT -> DOT;
				case QUOTED -> QUOTED;
			};
			for (int index = token.start(); index < token.end() && index < colors.length; index++) {
				colors[index] = color;
			}
		}
		return colors;
	}

	/** Draws the pattern on one line, cut with an ellipsis when it does not fit {@code width}. */
	public static void line(GuiGraphicsExtractor graphics, Font font, String pattern, int x, int y, int width) {
		int[] colors = colors(pattern);
		String shown = font.plainSubstrByWidth(pattern, width);
		boolean cut = shown.length() < pattern.length();
		if (cut) {
			shown = font.plainSubstrByWidth(pattern, Math.max(0, width - font.width("…")));
		}
		int end = range(graphics, font, pattern, colors, 0, shown.length(), x, y);
		if (cut) {
			graphics.text(font, "…", end, y, Theme.DIM_TEXT, false);
		}
	}

	/** Draws {@code [from, to)} in runs of equal colour; returns the x after the last character. */
	public static int range(GuiGraphicsExtractor graphics, Font font, String pattern, int[] colors, int from, int to, int x, int y) {
		int cursor = x;
		int runStart = from;
		for (int index = from + 1; index <= to; index++) {
			if (index == to || colors[index] != colors[runStart]) {
				String run = pattern.substring(runStart, index);
				graphics.text(font, run, cursor, y, colors[runStart], false);
				cursor += font.width(run);
				runStart = index;
			}
		}
		return cursor;
	}
}
