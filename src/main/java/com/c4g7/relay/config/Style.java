package com.c4g7.relay.config;

import com.google.gson.annotations.JsonAdapter;

/**
 * How a tab looks and treats its messages. There is one global style and every tab carries its own;
 * a tab set to "use global settings" reads and edits the global one, exactly like VelvetChat did.
 */
public final class Style {
	public static final int DEFAULT_BACKGROUND = 0x80000000;
	public static final int DEFAULT_MARKER = 0xFFFFD24A;
	public static final int DEFAULT_TIMESTAMP = 0xFFAAAAAA;
	public static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";

	public int lineLimit = 250;
	public boolean combineDuplicates = true;
	/** "Anti chat clear": ignore blank spam lines and keep this tab when the chat gets cleared. */
	public boolean keepOnClear = false;
	/** Shows vanilla's secure-chat indicator (the coloured bar and its tooltip) next to messages. */
	public boolean chatTrust = true;
	public boolean shadow = true;
	public boolean background = true;
	@JsonAdapter(HexColor.class)
	public int backgroundColor = DEFAULT_BACKGROUND;
	public boolean marker = false;
	@JsonAdapter(HexColor.class)
	public int markerColor = DEFAULT_MARKER;
	public boolean timestamps = false;
	@JsonAdapter(HexColor.class)
	public int timestampColor = DEFAULT_TIMESTAMP;
	public String timestampFormat = DEFAULT_TIME_FORMAT;

	public Style copy() {
		Style copy = new Style();
		copy.lineLimit = this.lineLimit;
		copy.combineDuplicates = this.combineDuplicates;
		copy.keepOnClear = this.keepOnClear;
		copy.chatTrust = this.chatTrust;
		copy.shadow = this.shadow;
		copy.background = this.background;
		copy.backgroundColor = this.backgroundColor;
		copy.marker = this.marker;
		copy.markerColor = this.markerColor;
		copy.timestamps = this.timestamps;
		copy.timestampColor = this.timestampColor;
		copy.timestampFormat = this.timestampFormat;
		return copy;
	}

	void sanitize() {
		this.lineLimit = Math.max(1, Math.min(this.lineLimit, 5000));
		if (this.timestampFormat == null || this.timestampFormat.isBlank()) {
			this.timestampFormat = DEFAULT_TIME_FORMAT;
		}
	}
}
