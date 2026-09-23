package com.c4g7.relay.mixin;

import com.c4g7.relay.ui.Settings;
import com.c4g7.relay.ui.WindowInput;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Routes the chat screen's mouse and keyboard input to Relay's windows before vanilla sees it. */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
	protected ChatScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void relay$mouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> callback) {
		ChatScreenInvoker screen = (ChatScreenInvoker) this;
		if (WindowInput.mouseClicked(event.x(), event.y(), event.button(), event.hasShiftDown(), screen::relay$handleComponentClicked)) {
			callback.setReturnValue(true);
		}
	}

	@Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
	private void relay$mouseScrolled(double x, double y, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> callback) {
		if (WindowInput.mouseScrolled(x, y, scrollY, this.minecraft.hasShiftDown())) {
			callback.setReturnValue(true);
		}
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void relay$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> callback) {
		if (Settings.keyPressed(event)) {
			callback.setReturnValue(true);
		}
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		return WindowInput.mouseDragged(event.x(), event.y()) || super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		return WindowInput.mouseReleased() || super.mouseReleased(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		String typed = event.codepointAsString();
		return !typed.isEmpty() && Settings.charTyped(typed) || super.charTyped(event);
	}
}
