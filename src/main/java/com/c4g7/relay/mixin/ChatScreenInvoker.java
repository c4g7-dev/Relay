package com.c4g7.relay.mixin;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChatScreen.class)
public interface ChatScreenInvoker {
	/** Runs a clicked text component exactly as vanilla chat does (links, commands, suggestions, ...). */
	@Invoker("handleComponentClicked")
	boolean relay$handleComponentClicked(Style clicked, boolean allowInsertions);
}
