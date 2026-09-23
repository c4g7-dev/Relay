package com.c4g7.relay.chat;

import com.c4g7.relay.config.Behaviour;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chat that outlives a connection: a divider line whenever you land on another server, and (when
 * enabled) the tabs' recent messages saved on exit and brought back on the next join.
 */
public final class Continuity {
	private static final Logger LOGGER = LoggerFactory.getLogger("Relay");
	private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
	private static final Gson GSON = new Gson();

	private final ChatHub hub;
	private final Path historyFile;
	private boolean reconfigured;
	private boolean restoredThisSession;
	@Nullable
	private RegistryAccess registries;

	public Continuity(ChatHub hub, Path historyFile) {
		this.hub = hub;
		this.historyFile = historyFile;
	}

	/** A proxy just moved us to another backend (vanilla restored its chat). */
	public void reconfigured() {
		this.reconfigured = true;
	}

	public void joined(Minecraft minecraft, RegistryAccess registries) {
		this.registries = registries;
		Behaviour behaviour = this.hub.config().behaviour;
		if (behaviour.restoreAfterRestart && !this.restoredThisSession) {
			this.restoredThisSession = true;
			this.restore(registries);
		}
		if (behaviour.serverDivider) {
			String clock = CLOCK.format(Instant.now().atZone(ZoneId.systemDefault()));
			String label = (this.reconfigured ? "⇄ " : "→ ") + serverName(minecraft) + " · " + clock;
			this.hub.divider(Component.literal(label).withStyle(ChatFormatting.GRAY));
		}
		this.reconfigured = false;
	}

	public void disconnected() {
		this.persist();
	}

	public void stopping() {
		this.persist();
	}

	private static String serverName(Minecraft minecraft) {
		ServerData server = minecraft.getCurrentServer();
		if (server != null) {
			return server.name == null || server.name.isBlank() ? server.ip : server.name;
		}
		if (minecraft.getSingleplayerServer() != null) {
			return minecraft.getSingleplayerServer().getWorldData().getLevelName();
		}
		return "?";
	}

	// ---- restore after restart ------------------------------------------------------------------

	private void persist() {
		Behaviour behaviour = this.hub.config().behaviour;
		try {
			if (!behaviour.restoreAfterRestart) {
				// Turning the option off also forgets what was saved.
				Files.deleteIfExists(this.historyFile);
				return;
			}
			JsonObject root = new JsonObject();
			root.addProperty("saved", System.currentTimeMillis());
			JsonObject tabs = new JsonObject();
			DynamicOps<JsonElement> ops = this.registries == null ? JsonOps.INSTANCE : RegistryOps.create(JsonOps.INSTANCE, this.registries);
			for (ChatTab tab : this.hub.tabs()) {
				JsonArray lines = new JsonArray();
				List<Entry> entries = tab.entries();
				for (Entry entry : entries.subList(Math.max(0, entries.size() - behaviour.restoreLimit), entries.size())) {
					Message message = entry.message();
					if (message.isDivider()) {
						continue;
					}
					JsonObject line = new JsonObject();
					line.addProperty("time", message.time());
					line.addProperty("text", message.plain());
					ComponentSerialization.CODEC.encodeStart(ops, message.content()).result().ifPresent(json -> line.add("component", json));
					lines.add(line);
				}
				if (!lines.isEmpty()) {
					tabs.add(tab.config().id.toString(), lines);
				}
			}
			root.add("tabs", tabs);
			Files.createDirectories(this.historyFile.getParent());
			Path temporary = this.historyFile.resolveSibling(this.historyFile.getFileName() + ".tmp");
			Files.writeString(temporary, GSON.toJson(root), StandardCharsets.UTF_8);
			Files.move(temporary, this.historyFile, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException | RuntimeException failed) {
			LOGGER.warn("Could not save the chat history", failed);
		}
	}

	private void restore(RegistryAccess registries) {
		if (!Files.isRegularFile(this.historyFile)) {
			return;
		}
		try {
			JsonObject root = JsonParser.parseString(Files.readString(this.historyFile, StandardCharsets.UTF_8)).getAsJsonObject();
			JsonObject tabs = root.getAsJsonObject("tabs");
			DynamicOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
			for (ChatTab tab : this.hub.tabs()) {
				JsonElement lines = tabs == null ? null : tabs.get(tab.config().id.toString());
				if (lines == null || !lines.isJsonArray() || tab.hasMessages()) {
					continue;
				}
				for (JsonElement element : lines.getAsJsonArray()) {
					JsonObject line = element.getAsJsonObject();
					Component content = line.has("component")
						? ComponentSerialization.CODEC.parse(ops, line.get("component")).result().orElse(null)
						: null;
					if (content == null) {
						content = Component.literal(line.has("text") ? line.get("text").getAsString() : "");
					}
					long time = line.has("time") ? line.get("time").getAsLong() : 0L;
					tab.add(new Message(content, null, time, false), null, tab.config().style(this.hub.config()));
				}
				tab.markRead();
			}
		} catch (IOException | RuntimeException failed) {
			LOGGER.warn("Could not restore the chat history", failed);
		}
	}
}
