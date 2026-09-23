package com.c4g7.relay.mixin;

import com.c4g7.relay.Relay;
import com.c4g7.relay.chat.ChatHub;
import com.c4g7.relay.ui.WindowRenderer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Feeds vanilla's chat into Relay and lets Relay draw it instead of the vanilla chat box. */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
	@Unique
	@Nullable
	private List<String> relay$sentHistory;

	@Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
		at = @At("HEAD"))
	private void relay$receive(Component content, @Nullable MessageSignature signature, GuiMessageSource source, @Nullable GuiMessageTag tag,
		CallbackInfo callback) {
		ChatHub hub = Relay.hub();
		if (hub != null) {
			try {
				hub.receive(content, tag);
			} catch (RuntimeException failed) {
				Relay.LOGGER.error("Could not route a chat message", failed);
			}
		}
	}

	@Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
		at = @At("HEAD"), cancellable = true)
	private void relay$render(GuiGraphicsExtractor graphics, Font font, int ticks, int mouseX, int mouseY, ChatComponent.DisplayMode displayMode,
		boolean changeCursorOnInsertions, CallbackInfo callback) {
		if (WindowRenderer.render(graphics, font, displayMode.foreground, mouseX, mouseY)) {
			callback.cancel();
		}
	}

	/** Vanilla's own (hidden) chat lines must not react to clicks while Relay draws the chat. */
	@Inject(method = "captureClickableText", at = @At("HEAD"), cancellable = true)
	private void relay$noHiddenClicks(ActiveTextCollector collector, int screenHeight, int ticks, ChatComponent.DisplayMode displayMode, CallbackInfo callback) {
		if (Relay.hub() != null && Relay.hub().config().behaviour.enabled) {
			callback.cancel();
		}
	}

	@Inject(method = "clearMessages", at = @At("HEAD"))
	private void relay$clear(boolean history, CallbackInfo callback) {
		ChatHub hub = Relay.hub();
		if (hub == null) {
			return;
		}
		hub.clear(history);
		// Leaving a server also wipes the ↑ history of what you typed; keep it when chat is kept.
		this.relay$sentHistory = history && hub.config().behaviour.keepAcrossServers
			? new ArrayList<>(((ChatComponent) (Object) this).getRecentChat())
			: null;
	}

	@Inject(method = "clearMessages", at = @At("TAIL"))
	private void relay$keepSentHistory(boolean history, CallbackInfo callback) {
		if (this.relay$sentHistory != null) {
			var recent = ((ChatComponent) (Object) this).getRecentChat();
			recent.clear();
			this.relay$sentHistory.forEach(recent::addLast);
			this.relay$sentHistory = null;
		}
	}

	@Inject(method = "storeState", at = @At("HEAD"))
	private void relay$snapshot(CallbackInfoReturnable<?> callback) {
		if (Relay.hub() != null) {
			Relay.hub().snapshotForReconfiguration();
		}
	}

	@Inject(method = "restoreState", at = @At("TAIL"))
	private void relay$restore(ChatComponent.State state, CallbackInfo callback) {
		if (Relay.hub() != null) {
			Relay.hub().restoreAfterReconfiguration();
			Relay.continuity().reconfigured();
		}
	}
}
