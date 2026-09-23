package com.c4g7.relay.ui.text;

import com.c4g7.relay.filter.RegexSyntax;
import com.c4g7.relay.ui.RegexPaint;
import com.c4g7.relay.ui.Theme;
import com.c4g7.relay.util.Rect;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * A soft-wrapping, syntax-coloured view of a regex {@link TextBuffer}. The pattern itself stays one
 * line (regexes have no newlines); it is only wrapped for display, and Up/Down move between the
 * wrapped rows like in any text editor.
 */
public final class PatternArea {
	public static final int LINE = 10;
	private static final long BLINK_MILLIS = 530L;
	private static final int BRACKET_MATCH = 0x50FFFFFF;

	private final TextBuffer buffer;
	private final Clicks clicks = new Clicks();
	private List<Integer> starts = List.of(0);
	private int wrapWidth = -1;
	private String wrappedText = null;
	private int firstLine;
	private long followedChange = -1L;
	private boolean dragging;

	public PatternArea(TextBuffer buffer) {
		this.buffer = buffer;
	}

	public TextBuffer buffer() {
		return this.buffer;
	}

	/** Number of wrapped rows the pattern needs at {@code width}. */
	public int lineCount(Font font, int width) {
		this.wrap(font, width);
		return this.starts.size();
	}

	private void wrap(Font font, int width) {
		String text = this.buffer.text();
		if (width == this.wrapWidth && text.equals(this.wrappedText)) {
			return;
		}
		List<Integer> lineStarts = new ArrayList<>();
		lineStarts.add(0);
		int lineWidth = 0;
		int lineStart = 0;
		for (int index = 0; index < text.length(); index++) {
			int glyph = font.width(String.valueOf(text.charAt(index)));
			if (lineWidth + glyph > width && index > lineStart) {
				lineStarts.add(index);
				lineStart = index;
				lineWidth = 0;
			}
			lineWidth += glyph;
		}
		this.starts = lineStarts;
		this.wrapWidth = width;
		this.wrappedText = text;
	}

	private int lineOf(int index) {
		int line = 0;
		for (int candidate = 1; candidate < this.starts.size(); candidate++) {
			if (this.starts.get(candidate) <= index) {
				line = candidate;
			}
		}
		return line;
	}

	private int lineEnd(int line, int length) {
		return line + 1 < this.starts.size() ? this.starts.get(line + 1) : length;
	}

	/**
	 * @param area the inner text area; as many rows are shown as fit, scrolling to follow the caret
	 * @param errorIndex character the compiler complained about, or -1
	 */
	public void render(GuiGraphicsExtractor graphics, Font font, Rect area, boolean focused, int errorIndex) {
		String text = this.buffer.text();
		this.wrap(font, area.width());
		int rows = Math.max(1, area.height() / LINE);
		int caretLine = this.lineOf(this.buffer.caret());
		// Follow the caret after it moves, but leave manual wheel scrolling alone otherwise.
		if (this.followedChange != this.buffer.changedAt()) {
			this.followedChange = this.buffer.changedAt();
			if (caretLine < this.firstLine) {
				this.firstLine = caretLine;
			} else if (caretLine >= this.firstLine + rows) {
				this.firstLine = caretLine - rows + 1;
			}
		}
		this.firstLine = Math.max(0, Math.min(this.firstLine, this.starts.size() - rows));

		int[] colors = RegexPaint.colors(text);
		int[] bracketPair = this.bracketPair(text);
		for (int row = 0; row < rows; row++) {
			int line = this.firstLine + row;
			if (line >= this.starts.size()) {
				break;
			}
			int start = this.starts.get(line);
			int end = this.lineEnd(line, text.length());
			int y = area.y() + row * LINE;
			for (int bracket : bracketPair) {
				if (bracket >= start && bracket < end) {
					int x0 = area.x() + font.width(text.substring(start, bracket));
					graphics.fill(x0 - 1, y - 1, x0 + font.width(String.valueOf(text.charAt(bracket))), y + 9, BRACKET_MATCH);
				}
			}
			if (focused && this.buffer.hasSelection()) {
				int from = Math.max(start, this.buffer.selectionStart());
				int to = Math.min(end, this.buffer.selectionEnd());
				if (from < to) {
					int x0 = area.x() + font.width(text.substring(start, from));
					int x1 = area.x() + font.width(text.substring(start, to));
					graphics.fill(x0, y - 1, x1, y + 9, Theme.SELECTION);
				}
			}
			RegexPaint.range(graphics, font, text, colors, start, end, area.x(), y);
			if (errorIndex >= start && (errorIndex < end || errorIndex == text.length() && line == this.starts.size() - 1)) {
				int x0 = area.x() + font.width(text.substring(start, Math.min(errorIndex, end)));
				int glyph = errorIndex < text.length() ? Math.max(3, font.width(String.valueOf(text.charAt(errorIndex)))) : 4;
				graphics.fill(x0, y + 9, x0 + glyph, y + 10, Theme.ERROR);
			}
			if (focused && line == caretLine && ((System.currentTimeMillis() - this.buffer.changedAt()) / BLINK_MILLIS) % 2 == 0) {
				int caretX = area.x() + font.width(text.substring(start, Math.min(this.buffer.caret(), end)));
				graphics.fill(caretX, y - 1, caretX + 1, y + 9, 0xFFFFFFFF);
			}
		}
		if (this.starts.size() > rows) {
			int track = area.height();
			int thumb = Math.max(4, track * rows / this.starts.size());
			int top = area.y() + (track - thumb) * this.firstLine / Math.max(1, this.starts.size() - rows);
			graphics.fill(area.right() + 2, area.y(), area.right() + 3, area.bottom(), Theme.SCROLL_TRACK);
			graphics.fill(area.right() + 2, top, area.right() + 3, top + thumb, Theme.SCROLL_THUMB);
		}
	}

	/** The bracket at or just before the caret and its partner, highlighted like in code editors. */
	private int[] bracketPair(String text) {
		if (this.buffer.hasSelection()) {
			return new int[0];
		}
		int caret = this.buffer.caret();
		for (int probe : new int[] {caret - 1, caret}) {
			if (probe >= 0 && probe < text.length() && "()[]".indexOf(text.charAt(probe)) >= 0) {
				int partner = RegexSyntax.matchingBracket(text, probe);
				if (partner >= 0) {
					return new int[] {probe, partner};
				}
			}
		}
		return new int[0];
	}

	public int indexAt(Font font, Rect area, double mouseX, double mouseY) {
		String text = this.buffer.text();
		this.wrap(font, area.width());
		int line = (int) Math.floor((mouseY - area.y()) / LINE) + this.firstLine;
		if (line < 0) {
			return 0;
		}
		if (line >= this.starts.size()) {
			return text.length();
		}
		return this.indexInLine(font, text, line, mouseX - area.x());
	}

	private int indexInLine(Font font, String text, int line, double offset) {
		int start = this.starts.get(line);
		int end = this.lineEnd(line, text.length());
		int before = 0;
		for (int index = start; index < end; index++) {
			int after = font.width(text.substring(start, index + 1));
			if (offset < (before + after) / 2.0) {
				return index;
			}
			before = after;
		}
		// The last row may place the caret after its final character; wrapped rows end before the break.
		return line == this.starts.size() - 1 ? end : Math.max(start, end - 1);
	}

	/** Moves the caret one wrapped row up or down, keeping its horizontal position. */
	public void vertical(Font font, int direction, boolean extend) {
		String text = this.buffer.text();
		int line = this.lineOf(this.buffer.caret());
		int target = line + direction;
		if (target < 0) {
			this.buffer.home(extend);
			return;
		}
		if (target >= this.starts.size()) {
			this.buffer.end(extend);
			return;
		}
		int x = font.width(text.substring(this.starts.get(line), this.buffer.caret()));
		this.buffer.moveTo(this.indexInLine(font, text, target, x), extend);
	}

	public void mouseDown(Font font, Rect area, double mouseX, double mouseY, boolean shift) {
		int index = this.indexAt(font, area, mouseX, mouseY);
		switch (this.clicks.register(mouseX, mouseY)) {
			case 2 -> this.buffer.selectWordAt(index);
			case 3 -> this.buffer.selectAll();
			default -> this.buffer.moveTo(index, shift);
		}
		this.dragging = true;
	}

	public boolean mouseDrag(Font font, Rect area, double mouseX, double mouseY) {
		if (!this.dragging) {
			return false;
		}
		this.buffer.moveTo(this.indexAt(font, area, mouseX, mouseY), true);
		return true;
	}

	public void mouseUp() {
		this.dragging = false;
	}

	public void scroll(int lines) {
		this.firstLine = Math.max(0, this.firstLine + lines);
	}
}
