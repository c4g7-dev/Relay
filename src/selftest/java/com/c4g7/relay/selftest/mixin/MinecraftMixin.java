package com.c4g7.relay.selftest.mixin;

import com.c4g7.relay.selftest.SelfTest;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Counts rendered frames, so the self-test can wait for real frames rather than wall-clock time. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Inject(method = "renderFrame", at = @At("TAIL"))
	private void relaySelftest$countFrame(boolean advanceGameTime, CallbackInfo ci) {
		SelfTest.frames++;
	}
}
