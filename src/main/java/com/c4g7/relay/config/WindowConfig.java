package com.c4g7.relay.config;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class WindowConfig {
	public Geometry geometry = new Geometry();
	public Visibility visibility = Visibility.ON_NEW_MESSAGE;
	/** A locked window cannot be dragged or resized until it is unlocked again. */
	public boolean locked = false;
	public UUID activeTab;
	public List<TabConfig> tabs = new ArrayList<>();

	public WindowConfig() {
	}

	public WindowConfig(Geometry geometry, TabConfig firstTab) {
		this.geometry = geometry;
		this.tabs.add(firstTab);
		this.activeTab = firstTab.id;
	}

	void sanitize() {
		if (this.geometry == null) {
			this.geometry = new Geometry();
		}
		this.geometry.sanitize();
		if (this.visibility == null) {
			this.visibility = Visibility.ON_NEW_MESSAGE;
		}
		if (this.tabs == null) {
			this.tabs = new ArrayList<>();
		}
		this.tabs.removeIf(tab -> tab == null);
		this.tabs.forEach(TabConfig::sanitize);
		if (this.tabs.stream().noneMatch(tab -> tab.id.equals(this.activeTab))) {
			this.activeTab = this.tabs.isEmpty() ? null : this.tabs.getFirst().id;
		}
	}
}
