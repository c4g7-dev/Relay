package com.c4g7.relay.config;

import java.util.ArrayList;
import java.util.List;

/** The whole of {@code config/relay/relay.json}. */
public final class RelayConfig {
	public static final int VERSION = 1;

	public int version = VERSION;
	public Behaviour behaviour = new Behaviour();
	public Style global = new Style();
	public List<WindowConfig> windows = new ArrayList<>();

	public static RelayConfig defaults() {
		RelayConfig config = new RelayConfig();
		config.sanitize();
		return config;
	}

	/**
	 * Repairs anything a hand edit or an old version could have broken: missing sections, empty
	 * windows, and the rule that exactly one tab across all windows is the Main tab.
	 */
	public void sanitize() {
		this.version = VERSION;
		if (this.behaviour == null) {
			this.behaviour = new Behaviour();
		}
		this.behaviour.sanitize();
		if (this.global == null) {
			this.global = new Style();
		}
		this.global.sanitize();
		if (this.windows == null) {
			this.windows = new ArrayList<>();
		}
		this.windows.removeIf(window -> window == null);
		this.windows.forEach(WindowConfig::sanitize);
		this.windows.removeIf(window -> window.tabs.isEmpty());

		boolean mainSeen = false;
		for (WindowConfig window : this.windows) {
			for (TabConfig tab : window.tabs) {
				if (tab.isMain()) {
					if (mainSeen) {
						tab.kind = TabConfig.Kind.CUSTOM;
					}
					mainSeen = true;
				}
			}
		}
		if (!mainSeen) {
			TabConfig main = new TabConfig(TabConfig.Kind.MAIN, "");
			if (this.windows.isEmpty()) {
				this.windows.add(new WindowConfig(new Geometry(), main));
			} else {
				this.windows.getFirst().tabs.addFirst(main);
			}
		}
	}

	public List<TabConfig> allTabs() {
		List<TabConfig> tabs = new ArrayList<>();
		this.windows.forEach(window -> tabs.addAll(window.tabs));
		return tabs;
	}
}
