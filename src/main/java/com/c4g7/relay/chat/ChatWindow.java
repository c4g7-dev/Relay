package com.c4g7.relay.chat;

import com.c4g7.relay.config.TabConfig;
import com.c4g7.relay.config.WindowConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChatWindow {
	private final WindowConfig config;
	private final List<ChatTab> tabs = new ArrayList<>();
	/** Lines scrolled up from the newest message. */
	private int scroll;

	ChatWindow(WindowConfig config) {
		this.config = config;
	}

	public WindowConfig config() {
		return this.config;
	}

	public List<ChatTab> tabs() {
		return Collections.unmodifiableList(this.tabs);
	}

	public ChatTab activeTab() {
		for (ChatTab tab : this.tabs) {
			if (tab.config().id.equals(this.config.activeTab)) {
				return tab;
			}
		}
		return this.tabs.isEmpty() ? null : this.tabs.getFirst();
	}

	public void activate(ChatTab tab) {
		if (this.tabs.contains(tab)) {
			this.config.activeTab = tab.config().id;
			tab.markRead();
			this.scroll = 0;
		}
	}

	public int scroll() {
		return this.scroll;
	}

	public void scroll(int lines) {
		this.scroll = Math.max(0, lines);
	}

	public boolean contains(ChatTab tab) {
		return this.tabs.contains(tab);
	}

	ChatTab attach(TabConfig config) {
		ChatTab tab = new ChatTab(config, this);
		this.tabs.add(tab);
		return tab;
	}

	void detach(ChatTab tab) {
		this.tabs.remove(tab);
		this.config.tabs.remove(tab.config());
		if (tab.config().id.equals(this.config.activeTab)) {
			this.config.activeTab = this.tabs.isEmpty() ? null : this.tabs.getFirst().config().id;
		}
	}

	public boolean hasMessages() {
		return this.tabs.stream().anyMatch(ChatTab::hasMessages);
	}
}
