package com.c4g7.relay.config;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TabConfig {
	public enum Kind {
		/** The one tab that receives everything no filter kept away from it. */
		MAIN,
		/** A tab that only shows messages caught by its own filters. */
		CUSTOM
	}

	public UUID id = UUID.randomUUID();
	public Kind kind = Kind.CUSTOM;
	public String name = "";
	public boolean useGlobal = true;
	public Style style = new Style();
	public List<FilterConfig> filters = new ArrayList<>();

	public TabConfig() {
	}

	public TabConfig(Kind kind, String name) {
		this.kind = kind;
		this.name = name;
	}

	public boolean isMain() {
		return this.kind == Kind.MAIN;
	}

	/** The style this tab currently renders with, and the one its settings rows edit. */
	public Style style(RelayConfig root) {
		return this.useGlobal ? root.global : this.style;
	}

	void sanitize() {
		if (this.id == null) {
			this.id = UUID.randomUUID();
		}
		if (this.kind == null) {
			this.kind = Kind.CUSTOM;
		}
		if (this.name == null) {
			this.name = "";
		}
		if (this.style == null) {
			this.style = new Style();
		}
		this.style.sanitize();
		if (this.filters == null) {
			this.filters = new ArrayList<>();
		}
		this.filters.removeIf(filter -> filter == null);
		this.filters.forEach(FilterConfig::sanitize);
	}
}
