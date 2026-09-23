package com.c4g7.relay.chat;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;
import org.jspecify.annotations.Nullable;

/** One received chat line, shared by every tab that shows it. */
public final class Message {
	private static final AtomicLong IDS = new AtomicLong();

	private final long id = IDS.incrementAndGet();
	private final Component content;
	@Nullable
	private final GuiMessageTag tag;
	private final long time;
	private final boolean divider;
	private String plain;
	private String json;

	public Message(Component content, @Nullable GuiMessageTag tag, long time, boolean divider) {
		this.content = content;
		this.tag = tag;
		this.time = time;
		this.divider = divider;
	}

	public static Message received(Component content, @Nullable GuiMessageTag tag) {
		return new Message(content, tag, System.currentTimeMillis(), false);
	}

	public static Message divider(Component label) {
		return new Message(label, null, System.currentTimeMillis(), true);
	}

	public long id() {
		return this.id;
	}

	public Component content() {
		return this.content;
	}

	@Nullable
	public GuiMessageTag tag() {
		return this.tag;
	}

	public long time() {
		return this.time;
	}

	/** A Relay-made separator line (server switch, restored history), not something the server sent. */
	public boolean isDivider() {
		return this.divider;
	}

	public String plain() {
		if (this.plain == null) {
			this.plain = this.content.getString();
		}
		return this.plain;
	}

	/** The message's JSON form, so filters can look inside hover text and click actions. */
	public String json() {
		if (this.json == null) {
			this.json = serialize(this.content, registries());
		}
		return this.json;
	}

	/** Plain text without legacy formatting codes, as copied to the clipboard. */
	public String copyText() {
		String text = this.plain();
		if (text.indexOf('§') < 0) {
			return text.strip();
		}
		StringBuilder plainText = new StringBuilder(text.length());
		for (int index = 0; index < text.length(); index++) {
			char character = text.charAt(index);
			if (character == '§') {
				index++;
			} else {
				plainText.append(character);
			}
		}
		return plainText.toString().strip();
	}

	static String serialize(Component component, @Nullable RegistryAccess registries) {
		try {
			DynamicOps<JsonElement> ops = registries == null ? JsonOps.INSTANCE : RegistryOps.create(JsonOps.INSTANCE, registries);
			return ComponentSerialization.CODEC.encodeStart(ops, component).result().map(JsonElement::toString).orElseGet(component::getString);
		} catch (RuntimeException failed) {
			return component.getString();
		}
	}

	@Nullable
	static RegistryAccess registries() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.getConnection() == null ? null : minecraft.getConnection().registryAccess();
	}
}
