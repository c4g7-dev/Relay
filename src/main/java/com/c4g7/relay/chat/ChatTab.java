package com.c4g7.relay.chat;

import com.c4g7.relay.config.Style;
import com.c4g7.relay.config.TabConfig;
import com.c4g7.relay.util.Tr;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

public final class ChatTab {
	private final TabConfig config;
	private final ChatWindow window;
	/** Oldest first. */
	private final List<Entry> entries = new ArrayList<>();
	private int unread;

	ChatTab(TabConfig config, ChatWindow window) {
		this.config = config;
		this.window = window;
	}

	public TabConfig config() {
		return this.config;
	}

	public ChatWindow window() {
		return this.window;
	}

	public List<Entry> entries() {
		return this.entries;
	}

	public int unread() {
		return this.unread;
	}

	public void markRead() {
		this.unread = 0;
	}

	public String title() {
		String name = this.config.name == null ? "" : this.config.name.strip();
		if (!name.isEmpty()) {
			return name;
		}
		return this.config.isMain() ? Tr.get("relaychat.tab.main", "Main") : Tr.get("relaychat.tab.new", "New tab");
	}

	void add(Message message, @Nullable Integer highlight, Style style) {
		Entry last = this.entries.isEmpty() ? null : this.entries.getLast();
		if (style.combineDuplicates && last != null && !message.isDivider() && !last.message().isDivider()
			&& last.message().content().equals(message.content())) {
			last.repeat(message, highlight);
		} else if (message.isDivider() && last != null && last.message().isDivider()) {
			// Two separators in a row say nothing; keep only the newer one.
			this.entries.set(this.entries.size() - 1, new Entry(message, null));
		} else {
			this.entries.add(new Entry(message, highlight));
		}
		this.trim(style.lineLimit);
		if (!message.isDivider() && this.window.activeTab() != this) {
			this.unread++;
		}
	}

	void trim(int limit) {
		int excess = this.entries.size() - Math.max(1, limit);
		if (excess > 0) {
			this.entries.subList(0, excess).clear();
		}
	}

	public boolean hasMessages() {
		return this.entries.stream().anyMatch(entry -> !entry.message().isDivider());
	}

	void clear() {
		this.entries.clear();
		this.unread = 0;
	}
}
