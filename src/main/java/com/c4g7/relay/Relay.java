package com.c4g7.relay;

import com.c4g7.relay.chat.ChatHub;
import com.c4g7.relay.chat.Continuity;
import com.c4g7.relay.config.ConfigStore;
import com.c4g7.relay.config.RelayConfig;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Relay implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("Relay");

	private static ChatHub hub;
	private static Continuity continuity;

	@Override
	public void onInitializeClient() {
		Path configDir = FabricLoader.getInstance().getConfigDir();
		ConfigStore store = new ConfigStore(configDir.resolve("relay"));
		RelayConfig config = store.load(configDir.resolve("velvetchat").resolve("chat.json"));
		hub = new ChatHub(store, config, Relay::playFilterSound);
		continuity = new Continuity(hub, configDir.resolve("relay").resolve("history.json"));
		if (FabricLoader.getInstance().isModLoaded("velvetchat")) {
			LOGGER.warn("VelvetChat is installed too. Both mods replace the chat, so remove VelvetChat to avoid doubled windows.");
		}

		RelayKeys.register();
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> continuity.joined(client, handler.registryAccess()));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> continuity.disconnected());
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			continuity.stopping();
			hub.save();
		});
		LOGGER.info("Relay ready with {} window(s) from {}", hub.windows().size(), store.file());
	}

	public static ChatHub hub() {
		return hub;
	}

	public static Continuity continuity() {
		return continuity;
	}

	private static void playFilterSound() {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
	}
}
