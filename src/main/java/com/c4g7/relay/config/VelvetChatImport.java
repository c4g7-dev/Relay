package com.c4g7.relay.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Converts a VelvetChat {@code chat.json} into a Relay config. Only the documented JSON layout is
 * read; unknown or missing keys fall back to Relay's defaults.
 */
public final class VelvetChatImport {
	private VelvetChatImport() {
	}

	public static RelayConfig read(String document) {
		JsonObject root = JsonParser.parseString(document).getAsJsonObject();
		RelayConfig config = new RelayConfig();
		JsonObject settings = object(root, "settings");
		config.behaviour.enabled = bool(settings, "enabled", true);
		config.behaviour.draggable = bool(settings, "draggable", true);
		config.behaviour.resizable = bool(settings, "resizeable", true);
		readStyle(settings, config.global, "global", true);

		for (JsonElement element : array(root, "windows")) {
			if (element.isJsonObject()) {
				config.windows.add(readWindow(element.getAsJsonObject()));
			}
		}
		config.sanitize();
		return config;
	}

	private static WindowConfig readWindow(JsonObject json) {
		WindowConfig window = new WindowConfig();
		Geometry geometry = window.geometry;
		geometry.width = (int) number(json, "width", 300);
		geometry.height = (int) number(json, "height", 150);
		boolean inside = !"OUTSIDE".equals(string(json, "boundsPosition", "INSIDE"));
		geometry.horizontal = switch (string(json, "horizontalAnchor", "LEFT")) {
			case "RIGHT" -> Geometry.Horizontal.RIGHT;
			case "CENTER" -> Geometry.Horizontal.CENTER;
			default -> Geometry.Horizontal.LEFT;
		};
		geometry.vertical = switch (string(json, "verticalAnchor", "BOTTOM")) {
			case "TOP" -> Geometry.Vertical.TOP;
			case "CENTER" -> Geometry.Vertical.CENTER;
			default -> Geometry.Vertical.BOTTOM;
		};
		// VelvetChat keeps percentages of the screen for windows inside it; pixel offsets otherwise.
		geometry.x = inside ? (float) number(json, "x", 0) / 100.0F : 0.0F;
		geometry.y = inside ? (float) number(json, "y", 0) / 100.0F : 0.0F;
		window.visibility = switch (string(json, "visibility", "ON_NEW_MESSAGE")) {
			case "ALWAYS" -> Visibility.ALWAYS;
			case "CHAT_OPEN" -> Visibility.CHAT_OPEN;
			default -> Visibility.ON_NEW_MESSAGE;
		};

		record Indexed(int index, TabConfig tab) {
		}
		List<Indexed> tabs = new ArrayList<>();
		for (JsonElement element : array(json, "tabs")) {
			if (element.isJsonObject()) {
				JsonObject tabJson = element.getAsJsonObject();
				tabs.add(new Indexed((int) number(tabJson, "index", tabs.size()), readTab(tabJson)));
			}
		}
		tabs.sort(Comparator.comparingInt(Indexed::index));
		int focused = (int) number(json, "focusedTab", 0);
		for (Indexed indexed : tabs) {
			window.tabs.add(indexed.tab());
			if (indexed.index() == focused) {
				window.activeTab = indexed.tab().id;
			}
		}
		return window;
	}

	private static TabConfig readTab(JsonObject json) {
		TabConfig tab = new TabConfig();
		tab.id = uuid(json, "uniqueId");
		tab.kind = "SERVER".equals(string(json, "type", "CUSTOM")) ? TabConfig.Kind.MAIN : TabConfig.Kind.CUSTOM;
		tab.name = string(json, "name", "");
		tab.useGlobal = bool(json, "global", true);
		readStyle(json, tab.style, "", false);
		for (JsonElement element : array(json, "filters")) {
			if (element.isJsonObject()) {
				tab.filters.add(readFilter(element.getAsJsonObject()));
			}
		}
		return tab;
	}

	private static FilterConfig readFilter(JsonObject json) {
		FilterConfig filter = new FilterConfig();
		filter.id = uuid(json, "id");
		filter.name = string(json, "name", "Filter");
		filter.include = strings(json, "include");
		filter.exclude = strings(json, "exclude");
		filter.highlight = bool(json, "shouldChangeBackground", false);
		filter.highlightColor = (int) number(json, "backgroundColor", 0);
		filter.sound = bool(json, "shouldPlaySound", false);
		filter.hide = bool(json, "shouldHideMessage", false);
		filter.searchJson = bool(json, "shouldFilterTooltip", false);
		filter.exclusive = bool(json, "keepOutOfMain", true);
		filter.caseSensitive = bool(json, "caseSensitive", false);
		filter.mode = bool(json, "advanced", false) ? FilterConfig.Mode.REGEX : FilterConfig.Mode.WORDS;
		filter.includeRegex = string(json, "includeRegEx", "");
		filter.excludeRegex = string(json, "excludeRegEx", "");
		// VelvetChat only ever matched a regex against the whole message; keep imported filters that way.
		filter.regexMatch = FilterConfig.RegexMatch.WHOLE;
		return filter;
	}

	/** Reads a style from either the {@code globalXxx} settings keys or a tab's plain keys. */
	private static void readStyle(JsonObject json, Style style, String prefix, boolean global) {
		style.lineLimit = (int) number(json, key(prefix, global, "chatLimit"), style.lineLimit);
		style.combineDuplicates = bool(json, key(prefix, global, "combineChatMessages"), style.combineDuplicates);
		style.keepOnClear = bool(json, key(prefix, global, "antiChatClear"), style.keepOnClear);
		style.chatTrust = bool(json, key(prefix, global, "chatTrust"), style.chatTrust);
		style.shadow = bool(json, key(prefix, global, "shadow"), style.shadow);
		style.background = bool(json, key(prefix, global, "background"), style.background);
		style.backgroundColor = (int) number(json, key(prefix, global, "backgroundColor"), style.backgroundColor);
		style.marker = bool(json, key(prefix, global, "showMessageMarker"), style.marker);
		style.markerColor = (int) number(json, key(prefix, global, "messageMarkerColor"), style.markerColor);
		style.timestamps = bool(json, key(prefix, global, "showTimestamp"), style.timestamps);
		style.timestampFormat = string(json, key(prefix, global, "timestampFormat"), style.timestampFormat);
		style.timestampColor = (int) number(json, key(prefix, global, "timestampColor"), style.timestampColor);
	}

	private static String key(String prefix, boolean global, String name) {
		return global ? prefix + Character.toUpperCase(name.charAt(0)) + name.substring(1) : name;
	}

	private static JsonObject object(JsonObject json, String key) {
		JsonElement value = json.get(key);
		return value != null && value.isJsonObject() ? value.getAsJsonObject() : new JsonObject();
	}

	private static JsonArray array(JsonObject json, String key) {
		JsonElement value = json.get(key);
		return value != null && value.isJsonArray() ? value.getAsJsonArray() : new JsonArray();
	}

	private static boolean bool(JsonObject json, String key, boolean fallback) {
		JsonElement value = json.get(key);
		return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean() ? value.getAsBoolean() : fallback;
	}

	private static double number(JsonObject json, String key, double fallback) {
		JsonElement value = json.get(key);
		return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber() ? value.getAsDouble() : fallback;
	}

	private static String string(JsonObject json, String key, String fallback) {
		JsonElement value = json.get(key);
		return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() ? value.getAsString() : fallback;
	}

	private static List<String> strings(JsonObject json, String key) {
		List<String> values = new ArrayList<>();
		for (JsonElement element : array(json, key)) {
			if (element.isJsonPrimitive()) {
				values.add(element.getAsString());
			}
		}
		return values;
	}

	private static UUID uuid(JsonObject json, String key) {
		try {
			return UUID.fromString(string(json, key, ""));
		} catch (IllegalArgumentException missing) {
			return UUID.randomUUID();
		}
	}
}
