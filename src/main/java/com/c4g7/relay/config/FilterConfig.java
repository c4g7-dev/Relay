package com.c4g7.relay.config;

import com.google.gson.annotations.JsonAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A rule that pulls matching messages into a tab and optionally decorates, sounds or hides them. */
public final class FilterConfig {
	public enum Mode {
		WORDS,
		REGEX
	}

	/** How a regex is applied: anywhere in the message, or against the message as a whole. */
	public enum RegexMatch {
		CONTAINS,
		WHOLE
	}

	public UUID id = UUID.randomUUID();
	public String name = "Filter";
	public Mode mode = Mode.WORDS;
	public List<String> include = new ArrayList<>();
	public List<String> exclude = new ArrayList<>();
	public String includeRegex = "";
	public String excludeRegex = "";
	public RegexMatch regexMatch = RegexMatch.CONTAINS;
	public boolean caseSensitive = false;
	/** Also search the message's JSON, so hover text and click actions can match ("Filter tooltips"). */
	public boolean searchJson = false;
	public boolean highlight = false;
	@JsonAdapter(HexColor.class)
	public int highlightColor = 0x00000000;
	public boolean sound = false;
	public boolean hide = false;
	/** "Keep out of Main": a message caught by this filter no longer shows up in the Main tab. */
	public boolean exclusive = true;

	public boolean regex() {
		return this.mode == Mode.REGEX;
	}

	public String displayName() {
		return this.name == null || this.name.isBlank() ? "Filter" : this.name;
	}

	void sanitize() {
		if (this.id == null) {
			this.id = UUID.randomUUID();
		}
		if (this.mode == null) {
			this.mode = Mode.WORDS;
		}
		if (this.regexMatch == null) {
			this.regexMatch = RegexMatch.CONTAINS;
		}
		if (this.include == null) {
			this.include = new ArrayList<>();
		}
		if (this.exclude == null) {
			this.exclude = new ArrayList<>();
		}
		this.include.removeIf(word -> word == null || word.isBlank());
		this.exclude.removeIf(word -> word == null || word.isBlank());
		if (this.includeRegex == null) {
			this.includeRegex = "";
		}
		if (this.excludeRegex == null) {
			this.excludeRegex = "";
		}
		if (this.name == null) {
			this.name = "Filter";
		}
	}
}
