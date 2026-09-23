package com.c4g7.relay.ui;

import com.c4g7.relay.util.Rect;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Drawing helpers: pixel-exact rounded shapes built from plain fills, plus colour maths. */
public final class Gfx {
	private Gfx() {
	}

	public static void fill(GuiGraphicsExtractor graphics, Rect area, int color) {
		graphics.fill(area.x(), area.y(), area.right(), area.bottom(), color);
	}

	public static void rounded(GuiGraphicsExtractor graphics, Rect area, int radius, int color) {
		rounded(graphics, area.x(), area.y(), area.right(), area.bottom(), radius, color, true);
	}

	public static void rounded(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom, int radius, int color) {
		rounded(graphics, left, top, right, bottom, radius, color, true);
	}

	/** A bar whose left end is rounded and right end square, the coloured state strip on settings rows. */
	public static void leftBar(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom, int radius, int color) {
		rounded(graphics, left, top, right, bottom, radius, color, false);
	}

	public static void circle(GuiGraphicsExtractor graphics, float left, float top, float size, int color) {
		int diameter = Math.max(1, (int) size);
		rounded(graphics, (int) left, (int) top, (int) left + diameter, (int) top + diameter, diameter / 2, color, true);
	}

	private static void rounded(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom, int radius, int color, boolean roundRight) {
		int height = bottom - top;
		if (height <= 0 || right <= left || (color >>> 24) == 0) {
			return;
		}
		int limited = Math.min(radius, Math.min(height, right - left) / 2);
		if (limited <= 0) {
			graphics.fill(left, top, right, bottom, color);
			return;
		}
		// Rows with the same inset are merged into one fill, so a panel costs a handful of quads.
		int runStart = 0;
		int runInset = inset(0, height, limited);
		for (int row = 1; row <= height; row++) {
			int rowInset = row < height ? inset(row, height, limited) : -1;
			if (rowInset != runInset) {
				int x0 = left + runInset;
				int x1 = roundRight ? right - runInset : right;
				if (x0 < x1) {
					graphics.fill(x0, top + runStart, x1, top + row, color);
				}
				runStart = row;
				runInset = rowInset;
			}
		}
	}

	/** How far row {@code y} of a shape {@code height} tall is pulled in by a corner of {@code radius}. */
	static int inset(int y, int height, int radius) {
		int fromEdge = Math.min(y, height - 1 - y);
		if (fromEdge >= radius) {
			return 0;
		}
		double dy = radius - fromEdge - 0.5;
		double reach = Math.sqrt(Math.max(0.0, radius * radius - dy * dy));
		return Math.max(0, (int) Math.round(radius - reach));
	}

	public static int alpha(int color, float alpha) {
		int base = color >>> 24;
		int scaled = Math.max(0, Math.min(255, Math.round(base * Math.max(0.0F, alpha))));
		return color & 0x00FFFFFF | scaled << 24;
	}

	/** Text alpha for fading chat lines; never fully transparent so the font does not fall back to opaque. */
	public static int textAlpha(int color, float alpha) {
		int value = Math.max(4, (int) (alpha * 255.0F));
		return color & 0x00FFFFFF | value << 24;
	}

	public static int blend(int from, int to, float progress) {
		float t = Math.max(0.0F, Math.min(1.0F, progress));
		int result = 0;
		for (int shift = 0; shift <= 24; shift += 8) {
			int a = from >> shift & 0xFF;
			int b = to >> shift & 0xFF;
			result |= Math.round(a + (b - a) * t) << shift;
		}
		return result;
	}

	public static String trim(Font font, String text, int width) {
		if (width <= 0 || text == null) {
			return "";
		}
		if (font.width(text) <= width) {
			return text;
		}
		int ellipsis = font.width("…");
		return font.plainSubstrByWidth(text, Math.max(0, width - ellipsis)) + "…";
	}

	/** Y for 9 px text vertically centred in {@code box}, with the 2 px optical nudge VelvetChat used. */
	public static int centredText(Rect box) {
		return (int) (box.y() + (box.height() - 9.0F) / 2.0F) + 2;
	}

	/** Y for text inside an input control; one pixel higher than labels. */
	public static int fieldText(Rect box) {
		return centredText(box) - 1;
	}
}
