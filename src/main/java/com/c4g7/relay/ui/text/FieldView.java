package com.c4g7.relay.ui.text;

import com.c4g7.relay.ui.Theme;
import com.c4g7.relay.util.Rect;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Draws a single-line {@link TextBuffer} inside a box and maps the mouse onto it: click to place the
 * caret, drag or Shift+click to select, double click for a word, triple click for everything. The
 * view scrolls sideways to keep the caret visible.
 */
public final class FieldView {
	private static final long BLINK_MILLIS = 530L;

	private final TextBuffer buffer;
	private final Clicks clicks = new Clicks();
	private int scroll;
	private boolean dragging;

	public FieldView(TextBuffer buffer) {
		this.buffer = buffer;
	}

	public TextBuffer buffer() {
		return this.buffer;
	}

	public boolean dragging() {
		return this.dragging;
	}

	/**
	 * @param area the text area (already inset inside its control)
	 * @param placeholder dim text shown while the buffer is empty and unfocused
	 */
	public void render(GuiGraphicsExtractor graphics, Font font, Rect area, int textY, boolean focused, int color, String placeholder) {
		this.render(graphics, font, area, textY, focused, color, placeholder, java.util.List.of(), 0);
	}

	/** As above, with {@code marks} ({@code [start, end)} spans) tinted in {@code markColor} behind the text. */
	public void render(GuiGraphicsExtractor graphics, Font font, Rect area, int textY, boolean focused, int color, String placeholder,
		java.util.List<int[]> marks, int markColor) {
		int width = area.width();
		if (width <= 0) {
			return;
		}
		String text = this.buffer.text();
		if (text.isEmpty() && !focused && placeholder != null) {
			graphics.text(font, placeholder, area.x(), textY, Theme.DIM_TEXT, false);
			return;
		}
		this.keepCaretVisible(font, width);
		String visible = font.plainSubstrByWidth(text.substring(this.scroll), width);
		int visibleEnd = this.scroll + visible.length();

		for (int[] mark : marks) {
			int from = Math.max(mark[0], this.scroll);
			int to = Math.min(mark[1], visibleEnd);
			if (from < to) {
				int x0 = area.x() + font.width(text.substring(this.scroll, from));
				int x1 = area.x() + font.width(text.substring(this.scroll, to));
				graphics.fill(x0, textY - 1, Math.min(x1, area.right()), textY + 9, markColor);
			}
		}
		if (focused && this.buffer.hasSelection()) {
			int from = Math.max(this.buffer.selectionStart(), this.scroll);
			int to = Math.min(this.buffer.selectionEnd(), visibleEnd);
			if (from < to) {
				int x0 = area.x() + font.width(text.substring(this.scroll, from));
				int x1 = area.x() + font.width(text.substring(this.scroll, to));
				graphics.fill(x0, textY - 1, Math.min(x1, area.right()), textY + 9, Theme.SELECTION);
			}
		}
		graphics.text(font, visible, area.x(), textY, color, false);
		if (focused && ((System.currentTimeMillis() - this.buffer.changedAt()) / BLINK_MILLIS) % 2 == 0) {
			int caretX = area.x() + font.width(text.substring(this.scroll, Math.min(this.buffer.caret(), visibleEnd)));
			graphics.fill(caretX, area.y() + 1, caretX + 1, area.bottom() - 1, 0xFFFFFFFF);
		}
	}

	public void mouseDown(Font font, Rect area, double mouseX, double mouseY, boolean shift) {
		int index = this.indexAt(font, area, mouseX);
		switch (this.clicks.register(mouseX, mouseY)) {
			case 2 -> this.buffer.selectWordAt(index);
			case 3 -> this.buffer.selectAll();
			default -> this.buffer.moveTo(index, shift);
		}
		this.dragging = true;
	}

	public void mouseDrag(Font font, Rect area, double mouseX) {
		if (this.dragging) {
			this.buffer.moveTo(this.indexAt(font, area, mouseX), true);
		}
	}

	public void mouseUp() {
		this.dragging = false;
	}

	/** Character boundary nearest to {@code mouseX}; past the edges it walks one character to auto-scroll. */
	public int indexAt(Font font, Rect area, double mouseX) {
		String text = this.buffer.text();
		if (mouseX < area.x()) {
			return Math.max(0, this.scroll - 1);
		}
		int offset = (int) mouseX - area.x();
		String visible = font.plainSubstrByWidth(text.substring(Math.min(this.scroll, text.length())), area.width());
		if (mouseX >= area.right() && this.scroll + visible.length() < text.length()) {
			return this.scroll + visible.length() + 1;
		}
		int before = 0;
		for (int index = 0; index < visible.length(); index++) {
			int after = font.width(visible.substring(0, index + 1));
			if (offset < (before + after) / 2.0) {
				return this.scroll + index;
			}
			before = after;
		}
		return this.scroll + visible.length();
	}

	private void keepCaretVisible(Font font, int width) {
		String text = this.buffer.text();
		int caret = this.buffer.caret();
		this.scroll = Math.max(0, Math.min(this.scroll, text.length()));
		if (caret < this.scroll) {
			this.scroll = caret;
		}
		while (this.scroll < caret && font.width(text.substring(this.scroll, caret)) > width - 1) {
			this.scroll++;
		}
		// Use free room on the right after deleting from the end.
		while (this.scroll > 0 && font.width(text.substring(this.scroll - 1)) <= width - 1) {
			this.scroll--;
		}
	}
}
