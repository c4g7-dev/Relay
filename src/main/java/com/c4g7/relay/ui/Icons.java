package com.c4g7.relay.ui;

import com.c4g7.relay.util.Rect;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class Icons {
	public static final Identifier MENU = icon("menu");
	public static final Identifier PLUS = icon("plus");
	public static final Identifier BIN = icon("bin");
	public static final Identifier CROSS = icon("cross");
	public static final Identifier BACK = icon("back");
	public static final Identifier FORWARD = icon("forward");
	public static final Identifier GEAR = icon("gear");
	public static final Identifier CHECK = icon("check");
	public static final Identifier LOCK = icon("lock");
	public static final Identifier UNLOCK = icon("unlock");
	public static final Identifier REGEX = icon("regex");
	public static final Identifier WINDOW = icon("window");
	public static final Identifier TAB = icon("tab");
	public static final Identifier SERVER = icon("server");

	private static final int TEXTURE = 128;
	private static final int MAX_SIZE = 12;

	private Icons() {
	}

	private static Identifier icon(String name) {
		return Identifier.fromNamespaceAndPath("relaychat", "textures/gui/icons/" + name + ".png");
	}

	/** Draws the icon centred in {@code area}, at most 12 px. */
	public static void draw(GuiGraphicsExtractor graphics, Identifier icon, Rect area, int color) {
		int size = Math.min(MAX_SIZE, Math.min(area.width(), area.height()));
		if (size <= 0 || (color >>> 24) == 0) {
			return;
		}
		int left = Math.round(area.x() + (area.width() - size) / 2.0F);
		int top = Math.round(area.y() + (area.height() - size) / 2.0F);
		graphics.blit(RenderPipelines.GUI_TEXTURED, icon, left, top, 0.0F, 0.0F, size, size, TEXTURE, TEXTURE, TEXTURE, TEXTURE, color);
	}
}
