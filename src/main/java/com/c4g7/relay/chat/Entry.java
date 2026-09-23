package com.c4g7.relay.chat;

import java.util.List;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/** A message as shown in one tab: it may be collapsed with repeats and tinted by a filter. */
public final class Entry {
	private Message message;
	private int repeats = 1;
	@Nullable
	private Integer highlight;
	private List<FormattedCharSequence> lines;
	private int linesKey;

	public Entry(Message message, @Nullable Integer highlight) {
		this.message = message;
		this.highlight = highlight;
	}

	public Message message() {
		return this.message;
	}

	public int repeats() {
		return this.repeats;
	}

	@Nullable
	public Integer highlight() {
		return this.highlight;
	}

	/** Folds an identical newer message into this line; the newest time wins so it shows again. */
	void repeat(Message newer, @Nullable Integer newerHighlight) {
		this.message = newer;
		this.repeats++;
		if (newerHighlight != null) {
			this.highlight = newerHighlight;
		}
		this.lines = null;
	}

	/** Wrapped lines cached for one layout; {@code key} captures width and style. */
	@Nullable
	public List<FormattedCharSequence> cachedLines(int key) {
		return this.lines != null && this.linesKey == key ? this.lines : null;
	}

	public void cacheLines(int key, List<FormattedCharSequence> lines) {
		this.linesKey = key;
		this.lines = lines;
	}
}
