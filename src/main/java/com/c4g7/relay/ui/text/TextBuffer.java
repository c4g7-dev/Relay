package com.c4g7.relay.ui.text;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Editing model behind every Relay text field: a single line of text with a caret, a selection
 * anchor and an undo history. It knows nothing about fonts or input devices, so it is shared by
 * the in-window settings fields and the regex editor, and it is unit tested on its own.
 *
 * <p>The selection is the range between {@link #anchor()} and {@link #caret()}; when both are equal
 * nothing is selected. Every mutating call replaces the selection through one private method, so
 * undo snapshots are taken in one place.
 */
public final class TextBuffer {
	private static final int UNDO_LIMIT = 200;
	private static final long TYPING_MERGE_MILLIS = 1200L;

	private final int maxLength;
	private final StringBuilder text;
	private final Deque<Snapshot> undo = new ArrayDeque<>();
	private final Deque<Snapshot> redo = new ArrayDeque<>();
	private int caret;
	private int anchor;
	private boolean typingRun;
	private boolean lastTypedSpace;
	private long lastTypedAt;
	private long changedAt;

	public TextBuffer(String initial, int maxLength) {
		this.maxLength = Math.max(1, maxLength);
		String start = initial == null ? "" : initial;
		this.text = new StringBuilder(start.length() > this.maxLength ? start.substring(0, this.maxLength) : start);
		this.caret = this.text.length();
		this.anchor = this.caret;
	}

	public String text() {
		return this.text.toString();
	}

	public int length() {
		return this.text.length();
	}

	public int maxLength() {
		return this.maxLength;
	}

	public int caret() {
		return this.caret;
	}

	public int anchor() {
		return this.anchor;
	}

	public boolean hasSelection() {
		return this.caret != this.anchor;
	}

	public int selectionStart() {
		return Math.min(this.caret, this.anchor);
	}

	public int selectionEnd() {
		return Math.max(this.caret, this.anchor);
	}

	public String selectedText() {
		return this.text.substring(this.selectionStart(), this.selectionEnd());
	}

	/** Milliseconds timestamp of the last caret move or edit, used to restart the caret blink. */
	public long changedAt() {
		return this.changedAt;
	}

	// ---- caret movement ------------------------------------------------------------------------

	public void moveTo(int position, boolean extend) {
		this.caret = clamp(position);
		if (!extend) {
			this.anchor = this.caret;
		}
		this.endTypingRun();
		this.touch();
	}

	public void select(int from, int to) {
		this.anchor = clamp(from);
		this.caret = clamp(to);
		this.endTypingRun();
		this.touch();
	}

	public void selectAll() {
		this.select(0, this.text.length());
	}

	/** Selects the word (or run of punctuation / whitespace) around {@code position}, as a double click does. */
	public void selectWordAt(int position) {
		int at = clamp(position);
		if (this.text.isEmpty()) {
			return;
		}
		int probe = at == this.text.length() ? at - 1 : at;
		int kind = kindOf(this.text.charAt(probe));
		int start = probe;
		while (start > 0 && kindOf(this.text.charAt(start - 1)) == kind) {
			start--;
		}
		int end = probe + 1;
		while (end < this.text.length() && kindOf(this.text.charAt(end)) == kind) {
			end++;
		}
		this.select(start, end);
	}

	public void left(boolean extend, boolean word) {
		if (this.hasSelection() && !extend) {
			this.moveTo(this.selectionStart(), false);
		} else {
			this.moveTo(word ? this.previousWordBoundary(this.caret) : this.caret - 1, extend);
		}
	}

	public void right(boolean extend, boolean word) {
		if (this.hasSelection() && !extend) {
			this.moveTo(this.selectionEnd(), false);
		} else {
			this.moveTo(word ? this.nextWordBoundary(this.caret) : this.caret + 1, extend);
		}
	}

	public void home(boolean extend) {
		this.moveTo(0, extend);
	}

	public void end(boolean extend) {
		this.moveTo(this.text.length(), extend);
	}

	// ---- editing --------------------------------------------------------------------------------

	/** Types text at the caret, replacing the selection. Consecutive typing merges into one undo step. */
	public void type(String typed) {
		if (typed == null || typed.isEmpty()) {
			return;
		}
		long now = System.currentTimeMillis();
		boolean space = typed.isBlank();
		// A new word starts a new undo step, the way text editors group typing.
		boolean merge = this.typingRun && !this.hasSelection() && now - this.lastTypedAt < TYPING_MERGE_MILLIS
			&& !(this.lastTypedSpace && !space);
		this.edit(typed, !merge);
		this.typingRun = true;
		this.lastTypedSpace = space;
		this.lastTypedAt = now;
	}

	/** Inserts text (a paste or a snippet) as its own undo step, replacing the selection. */
	public void insert(String inserted) {
		if (inserted == null || inserted.isEmpty()) {
			return;
		}
		this.edit(inserted, true);
		this.endTypingRun();
	}

	/** Wraps the selection in {@code before}/{@code after}, or inserts both around the caret. */
	public void wrap(String before, String after) {
		String inner = this.selectedText();
		int start = this.selectionStart();
		this.edit(before + inner + after, true);
		this.endTypingRun();
		if (inner.isEmpty()) {
			this.caret = start + before.length();
			this.anchor = this.caret;
		} else {
			this.anchor = start + before.length();
			this.caret = this.anchor + inner.length();
		}
	}

	public void backspace(boolean word) {
		if (this.hasSelection()) {
			this.edit("", true);
		} else if (this.caret > 0) {
			this.anchor = word ? this.previousWordBoundary(this.caret) : this.caret - 1;
			this.edit("", true);
		}
		this.endTypingRun();
	}

	public void delete(boolean word) {
		if (this.hasSelection()) {
			this.edit("", true);
		} else if (this.caret < this.text.length()) {
			this.anchor = word ? this.nextWordBoundary(this.caret) : this.caret + 1;
			this.edit("", true);
		}
		this.endTypingRun();
	}

	/** Removes and returns the selection, or an empty string when nothing is selected. */
	public String cut() {
		if (!this.hasSelection()) {
			return "";
		}
		String removed = this.selectedText();
		this.edit("", true);
		this.endTypingRun();
		return removed;
	}

	public void setText(String replacement) {
		this.select(0, this.text.length());
		this.edit(replacement == null ? "" : replacement, true);
		this.endTypingRun();
	}

	public boolean undo() {
		return this.travel(this.undo, this.redo);
	}

	public boolean redo() {
		return this.travel(this.redo, this.undo);
	}

	public boolean canUndo() {
		return !this.undo.isEmpty();
	}

	// ---- internals ------------------------------------------------------------------------------

	private void edit(String replacement, boolean snapshot) {
		int start = this.selectionStart();
		int end = this.selectionEnd();
		int room = this.maxLength - (this.text.length() - (end - start));
		String fitting = replacement.length() > room ? replacement.substring(0, Math.max(0, room)) : replacement;
		if (start == end && fitting.isEmpty()) {
			return;
		}
		if (snapshot) {
			this.remember();
		}
		this.text.replace(start, end, fitting);
		this.caret = start + fitting.length();
		this.anchor = this.caret;
		this.touch();
	}

	private void remember() {
		this.undo.push(new Snapshot(this.text.toString(), this.caret, this.anchor));
		while (this.undo.size() > UNDO_LIMIT) {
			this.undo.removeLast();
		}
		this.redo.clear();
	}

	private boolean travel(Deque<Snapshot> from, Deque<Snapshot> to) {
		if (from.isEmpty()) {
			return false;
		}
		to.push(new Snapshot(this.text.toString(), this.caret, this.anchor));
		Snapshot state = from.pop();
		this.text.setLength(0);
		this.text.append(state.text());
		this.caret = clampTo(state.caret(), this.text.length());
		this.anchor = clampTo(state.anchor(), this.text.length());
		this.endTypingRun();
		this.touch();
		return true;
	}

	private void endTypingRun() {
		this.typingRun = false;
	}

	private void touch() {
		this.changedAt = System.currentTimeMillis();
	}

	int previousWordBoundary(int from) {
		int index = clamp(from);
		while (index > 0 && Character.isWhitespace(this.text.charAt(index - 1))) {
			index--;
		}
		if (index == 0) {
			return 0;
		}
		int kind = kindOf(this.text.charAt(index - 1));
		while (index > 0 && kindOf(this.text.charAt(index - 1)) == kind) {
			index--;
		}
		return index;
	}

	int nextWordBoundary(int from) {
		int index = clamp(from);
		int length = this.text.length();
		if (index < length) {
			int kind = kindOf(this.text.charAt(index));
			while (index < length && kindOf(this.text.charAt(index)) == kind) {
				index++;
			}
		}
		while (index < length && Character.isWhitespace(this.text.charAt(index))) {
			index++;
		}
		return index;
	}

	private static int kindOf(char character) {
		if (Character.isWhitespace(character)) {
			return 0;
		}
		return Character.isLetterOrDigit(character) || character == '_' ? 1 : 2;
	}

	private int clamp(int position) {
		return clampTo(position, this.text.length());
	}

	private static int clampTo(int position, int length) {
		return Math.max(0, Math.min(position, length));
	}

	private record Snapshot(String text, int caret, int anchor) {
	}
}
