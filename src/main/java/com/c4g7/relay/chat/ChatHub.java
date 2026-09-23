package com.c4g7.relay.chat;

import com.c4g7.relay.config.ConfigStore;
import com.c4g7.relay.config.FilterConfig;
import com.c4g7.relay.config.Geometry;
import com.c4g7.relay.config.RelayConfig;
import com.c4g7.relay.config.Style;
import com.c4g7.relay.config.TabConfig;
import com.c4g7.relay.config.WindowConfig;
import com.c4g7.relay.filter.FilterMatcher;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Owns the windows and tabs at runtime and decides which tab gets which message. Routing follows
 * VelvetChat's rules: the Main tab gets everything that no "keep out of Main" or "hide" filter
 * claimed, and every other tab only gets what its own filters catch.
 */
public final class ChatHub {
	/** How many received messages the regex editor can preview against. */
	public static final int RECENT_LIMIT = 1000;

	private final ConfigStore store;
	private final RelayConfig config;
	private final Runnable filterSound;
	private final List<ChatWindow> windows = new ArrayList<>();
	private final Deque<Message> recent = new ArrayDeque<>();
	@Nullable
	private Map<UUID, List<Entry>> reconfigurationSnapshot;
	private final Set<UUID> clearedSinceSnapshot = new HashSet<>();

	public ChatHub(ConfigStore store, RelayConfig config, Runnable filterSound) {
		this.store = store;
		this.config = config;
		this.filterSound = filterSound;
		for (WindowConfig windowConfig : config.windows) {
			ChatWindow window = new ChatWindow(windowConfig);
			windowConfig.tabs.forEach(window::attach);
			this.windows.add(window);
		}
	}

	public RelayConfig config() {
		return this.config;
	}

	public List<ChatWindow> windows() {
		return Collections.unmodifiableList(this.windows);
	}

	/** Every message received this session, newest first, capped at {@link #RECENT_LIMIT}. */
	public Deque<Message> recent() {
		return this.recent;
	}

	public void save() {
		this.store.save(this.config);
	}

	public List<ChatTab> tabs() {
		List<ChatTab> tabs = new ArrayList<>();
		this.windows.forEach(window -> tabs.addAll(window.tabs()));
		return tabs;
	}

	public boolean isMainWindow(ChatWindow window) {
		return window.tabs().stream().anyMatch(tab -> tab.config().isMain());
	}

	// ---- structure ------------------------------------------------------------------------------

	public ChatWindow createWindow() {
		TabConfig tab = new TabConfig(TabConfig.Kind.CUSTOM, "");
		WindowConfig windowConfig = new WindowConfig(
			new Geometry(Geometry.Horizontal.CENTER, Geometry.Vertical.CENTER, 0.0F, 0.0F, 200, 80), tab);
		this.config.windows.add(windowConfig);
		ChatWindow window = new ChatWindow(windowConfig);
		window.attach(tab);
		this.windows.add(window);
		this.save();
		return window;
	}

	public boolean canDelete(ChatWindow window) {
		return this.windows.size() > 1 && !this.isMainWindow(window);
	}

	public void deleteWindow(ChatWindow window) {
		if (this.canDelete(window)) {
			this.windows.remove(window);
			this.config.windows.remove(window.config());
			this.save();
		}
	}

	public ChatTab createTab(ChatWindow window) {
		TabConfig config = new TabConfig(TabConfig.Kind.CUSTOM, "");
		window.config().tabs.add(config);
		ChatTab tab = window.attach(config);
		this.save();
		return tab;
	}

	public void deleteTab(ChatTab tab) {
		if (tab.config().isMain()) {
			return;
		}
		ChatWindow window = tab.window();
		window.detach(tab);
		if (window.tabs().isEmpty()) {
			this.windows.remove(window);
			this.config.windows.remove(window.config());
		}
		this.save();
	}

	// ---- messages -------------------------------------------------------------------------------

	public void receive(Component content, @Nullable GuiMessageTag tag) {
		Message message = Message.received(content, tag);
		this.recent.addFirst(message);
		while (this.recent.size() > RECENT_LIMIT) {
			this.recent.removeLast();
		}

		List<ChatTab> tabs = this.tabs();
		Map<ChatTab, List<FilterConfig>> hits = new HashMap<>();
		boolean hidden = false;
		boolean claimed = false;
		for (ChatTab tab : tabs) {
			List<FilterConfig> matched = new ArrayList<>();
			for (FilterConfig filter : tab.config().filters) {
				if (FilterMatcher.matches(filter, message.plain(), message::json)) {
					matched.add(filter);
					hidden |= filter.hide;
					claimed |= !tab.config().isMain() && filter.exclusive;
				}
			}
			hits.put(tab, matched);
		}

		boolean blank = message.plain().isBlank();
		boolean sound = false;
		for (ChatTab tab : tabs) {
			Style style = tab.config().style(this.config);
			if (blank && style.keepOnClear) {
				continue;
			}
			List<FilterConfig> matched = hits.get(tab);
			if (!matched.isEmpty()) {
				if (matched.stream().anyMatch(filter -> filter.hide)) {
					continue;
				}
				Integer highlight = matched.stream().filter(filter -> filter.highlight).findFirst().map(filter -> filter.highlightColor).orElse(null);
				sound |= matched.stream().anyMatch(filter -> filter.sound);
				tab.add(message, highlight, style);
			} else if (tab.config().isMain() && !hidden && !claimed) {
				tab.add(message, null, style);
			}
		}
		if (sound) {
			this.filterSound.run();
		}
	}

	/** Adds a separator line to every tab that already shows something. */
	public void divider(Component label) {
		Message divider = Message.divider(label);
		for (ChatTab tab : this.tabs()) {
			if (tab.hasMessages()) {
				tab.add(divider, null, tab.config().style(this.config));
			}
		}
	}

	/**
	 * Called whenever vanilla clears its chat.
	 *
	 * @param disconnect true when you leave a server or a proxy moves you (vanilla also wipes the
	 *     input history then); false for a deliberate clear such as F3+D, which always empties Relay
	 */
	public void clear(boolean disconnect) {
		if (!disconnect) {
			this.tabs().forEach(ChatTab::clear);
			this.recent.clear();
			return;
		}
		if (this.config.behaviour.keepAcrossServers) {
			return;
		}
		for (ChatTab tab : this.tabs()) {
			if (!tab.config().style(this.config).keepOnClear) {
				tab.clear();
				this.clearedSinceSnapshot.add(tab.config().id);
			}
		}
	}

	/** A proxy is about to move us to another backend: vanilla saves its chat, and so do we. */
	public void snapshotForReconfiguration() {
		Map<UUID, List<Entry>> snapshot = new HashMap<>();
		for (ChatTab tab : this.tabs()) {
			snapshot.put(tab.config().id, new ArrayList<>(tab.entries()));
		}
		this.reconfigurationSnapshot = snapshot;
		this.clearedSinceSnapshot.clear();
	}

	/** The move finished and vanilla restored its chat; put back what the clear in between removed. */
	public void restoreAfterReconfiguration() {
		Map<UUID, List<Entry>> snapshot = this.reconfigurationSnapshot;
		this.reconfigurationSnapshot = null;
		if (snapshot == null) {
			return;
		}
		for (ChatTab tab : this.tabs()) {
			List<Entry> before = snapshot.get(tab.config().id);
			if (before != null && this.clearedSinceSnapshot.contains(tab.config().id)) {
				tab.entries().addAll(0, before);
				tab.trim(tab.config().style(this.config).lineLimit);
			}
		}
		this.clearedSinceSnapshot.clear();
	}
}
