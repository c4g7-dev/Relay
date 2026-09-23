package com.c4g7.relay.ui;

import com.c4g7.relay.Relay;
import com.c4g7.relay.chat.ChatHub;
import com.c4g7.relay.chat.ChatTab;
import com.c4g7.relay.chat.ChatWindow;
import com.c4g7.relay.chat.Entry;
import com.c4g7.relay.config.Style;
import com.c4g7.relay.config.Visibility;
import com.c4g7.relay.util.Rect;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.util.FormattedCharSequence;

/** Draws every chat window: background, tab bar, edge glow, messages or the settings panel. */
public final class WindowRenderer {
	private static final int DIVIDER_RULE = 0x60A0A0A0;

	private record Anim(Tween left, Tween width, Tween glow) {
		Anim() {
			this(new Tween(0.0F, Theme.TAB_SLIDE), new Tween(0.0F, Theme.TAB_SLIDE), new Tween(0.0F, Theme.EDGE_FADE));
		}
	}

	private static final Map<ChatWindow, Anim> ANIMS = new WeakHashMap<>();

	private WindowRenderer() {
	}

	/** @return false when Relay is switched off and vanilla should draw its chat instead */
	public static boolean render(GuiGraphicsExtractor graphics, Font font, boolean chatOpen, int mouseX, int mouseY) {
		ChatHub hub = Relay.hub();
		if (hub == null || !hub.config().behaviour.enabled) {
			return false;
		}
		if (!chatOpen) {
			Menu.close();
			Settings.commitAll();
		}
		Rect screen = Frame.screen();
		List<ChatWindow> windows = new ArrayList<>(hub.windows());
		for (ChatWindow window : windows) {
			renderWindow(graphics, font, window, screen, chatOpen, mouseX, mouseY);
		}
		if (chatOpen) {
			Menu.render(graphics, font, mouseX, mouseY);
			requestCursor(graphics, windows, mouseX, mouseY);
		}
		return true;
	}

	private static void renderWindow(GuiGraphicsExtractor graphics, Font font, ChatWindow window, Rect screen, boolean chatOpen, int mouseX, int mouseY) {
		Visibility visibility = window.config().visibility;
		if (!visibility.drawnWhile(chatOpen)) {
			return;
		}
		Rect bounds = Frame.confine(window.config().geometry.resolve(screen.width(), screen.height()), screen);
		Rect body = Frame.body(bounds);
		ChatTab tab = window.activeTab();
		Style style = tab == null ? Relay.hub().config().global : tab.config().style(Relay.hub().config());
		boolean everything = visibility.showsEverything(chatOpen);
		boolean settings = chatOpen && Settings.isOpen(window);
		boolean background = style.background || settings;
		int backgroundColor = settings ? Theme.PANEL : style.backgroundColor;
		if (background && chatOpen) {
			Gfx.fill(graphics, body, backgroundColor);
		}
		if (chatOpen) {
			renderTabBar(graphics, font, window, bounds);
			renderEdge(graphics, font, window, bounds, mouseX, mouseY);
		}
		if (settings) {
			Settings.render(window, graphics, font, body, mouseX, mouseY);
		} else if (tab != null) {
			renderMessages(graphics, font, window, tab, style, body, everything, chatOpen, background && !chatOpen ? backgroundColor : 0, mouseX, mouseY);
		}
	}

	// ---- tab bar --------------------------------------------------------------------------------

	private static void renderTabBar(GuiGraphicsExtractor graphics, Font font, ChatWindow window, Rect bounds) {
		long now = System.currentTimeMillis();
		Anim anim = ANIMS.computeIfAbsent(window, key -> new Anim());
		graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.y() + Theme.TAB_BAR_HEIGHT, Theme.TAB_BAR);
		boolean locked = window.config().locked;
		int barRight = TabBar.limit(bounds, locked);
		ChatTab active = window.activeTab();
		for (TabBar.Button button : TabBar.layout(window, bounds, font)) {
			if (button.x() >= barRight) {
				break;
			}
			int right = Math.min(button.x() + button.width(), barRight);
			boolean isActive = button.tab() == active;
			if (isActive) {
				graphics.fill(button.x(), button.y(), right, button.y() + Theme.TAB_BAR_HEIGHT, Theme.ACTIVE_TAB);
				anim.left().target(button.x() - bounds.x(), now);
				anim.width().target(right - button.x(), now);
			}
			if (!isActive && button.tab().unread() > 0) {
				int markLeft = right - 3 - 1;
				graphics.fill(markLeft, button.y() + 1, markLeft + 3, button.y() + 4, Theme.TEXT);
			}
			int labelLeft = button.x() + 4;
			graphics.text(font, Gfx.trim(font, button.label(), right - labelLeft - 2), labelLeft, button.y() + 3, isActive ? Theme.TEXT : Theme.INACTIVE_TEXT, false);
		}
		float offset = anim.left().value(now);
		float width = anim.width().value(now);
		if (width > 0.0F) {
			int left = (int) (bounds.x() + offset);
			int barBottom = bounds.y() + Theme.TAB_BAR_HEIGHT;
			graphics.fill(left, barBottom - 2, (int) (left + width), barBottom, Theme.ACCENT);
		}
		if (locked) {
			Icons.draw(graphics, Icons.LOCK, Frame.lockBadge(bounds).inset(2), Theme.DIM_TEXT);
		}
		Icons.draw(graphics, Icons.MENU, Frame.menuButton(bounds).inset(2), Theme.TEXT);
	}

	private static void renderEdge(GuiGraphicsExtractor graphics, Font font, ChatWindow window, Rect bounds, int mouseX, int mouseY) {
		Frame.Edge edge = WindowInput.highlightedEdge(window, bounds, mouseX, mouseY);
		long now = System.currentTimeMillis();
		Tween glow = ANIMS.computeIfAbsent(window, key -> new Anim()).glow();
		float lit = glow.value(now);
		glow.target(edge == null ? 0.0F : 1.0F, now);
		if (edge == null || lit <= 0.0F) {
			return;
		}
		int color = Gfx.alpha(Theme.TEXT, lit);
		if (WindowInput.isResizing(window)) {
			switch (edge) {
				case LEFT -> graphics.fill(bounds.x() - 1, bounds.y(), bounds.x(), bounds.bottom(), color);
				case RIGHT -> graphics.fill(bounds.right(), bounds.y(), bounds.right() + 1, bounds.bottom(), color);
				case TOP -> graphics.fill(bounds.x(), bounds.y() - 1, bounds.right(), bounds.y(), color);
				case BOTTOM -> graphics.fill(bounds.x(), bounds.bottom(), bounds.right(), bounds.bottom() + 1, color);
			}
		}
		boolean horizontal = edge.horizontal();
		graphics.text(font, horizontal ? "||" : "=", mouseX - (horizontal ? 1 : 2), mouseY - (horizontal ? 2 : 3), color, false);
	}

	// ---- messages -------------------------------------------------------------------------------

	private static void renderMessages(GuiGraphicsExtractor graphics, Font font, ChatWindow window, ChatTab tab, Style style, Rect body,
		boolean everything, boolean chatOpen, int lineBackground, int mouseX, int mouseY) {
		int indent = style.marker ? 4 : 0;
		int textLeft = body.x() + Theme.PADDING + indent;
		int width = body.width() - 2 * Theme.PADDING - indent;
		if (width <= 0) {
			return;
		}
		List<Lines.Line> lines = Lines.of(tab, font, width, style, everything);
		int scroll = Math.min(window.scroll(), Math.max(0, lines.size() - body.height() / Theme.LINE_HEIGHT));
		window.scroll(scroll);
		int[] range = Lines.window(body, lines.size(), scroll);
		int hovered = chatOpen && body.contains(mouseX, mouseY) ? Lines.at(body, lines.size(), scroll, mouseY) : -1;
		int y = body.bottom() - Theme.LINE_HEIGHT;
		int topmost = Integer.MAX_VALUE;
		float topmostAlpha = 0.0F;
		for (int index = range[1] - 1; index >= range[0]; index--) {
			Lines.Line line = lines.get(index);
			Entry entry = line.entry();
			topmost = y;
			topmostAlpha = line.alpha();
			if (lineBackground != 0) {
				graphics.fill(body.x(), y, body.right(), y + Theme.LINE_HEIGHT, Gfx.alpha(lineBackground, line.alpha()));
			}
			if (entry.highlight() != null) {
				graphics.fill(body.x(), y, body.right(), y + Theme.LINE_HEIGHT, Gfx.alpha(entry.highlight(), line.alpha()));
			}
			GuiMessageTag tag = entry.message().tag();
			// Every server system message carries a grey "System" tag; only flag the ones that mean something.
			if (style.chatTrust && tag != null && !"System".equals(tag.logTag())) {
				int bar = (Math.round(line.alpha() * 255.0F) << 24) | (tag.indicatorColor() & 0xFFFFFF);
				graphics.fill(body.x(), y, body.x() + 1, y + Theme.LINE_HEIGHT, bar);
				if (index == hovered && mouseX < body.x() + 2 && tag.text() != null) {
					graphics.setTooltipForNextFrame(font, tag.text(), mouseX, mouseY);
				}
			}
			if (style.marker && line.first()) {
				int left = body.x() + 2;
				graphics.fill(left, y, left + 2, y + Theme.LINE_HEIGHT - 2, Gfx.textAlpha(style.markerColor, line.alpha()));
			}
			if (entry.message().isDivider()) {
				renderDivider(graphics, font, line.text(), body, y, line.alpha());
			} else {
				graphics.text(font, line.text(), textLeft, y, Gfx.textAlpha(Theme.TEXT, line.alpha()), style.shadow);
				if (index == hovered) {
					// An invisible pass over the hovered line gives vanilla's hover tooltips and click cursor.
					ActiveTextCollector collector = graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.TOOLTIP_AND_CURSOR);
					collector.accept(TextAlignment.LEFT, textLeft, y, collector.defaultParameters().withOpacity(0.0F), line.text());
				}
			}
			y -= Theme.LINE_HEIGHT;
		}
		if (lineBackground != 0 && topmost != Integer.MAX_VALUE) {
			graphics.fill(body.x(), topmost - 1, body.right(), topmost, Gfx.alpha(lineBackground, topmostAlpha));
		}
		if (chatOpen && scroll > 0) {
			renderScrollHint(graphics, body, lines.size(), scroll);
		}
	}

	private static void renderDivider(GuiGraphicsExtractor graphics, Font font, FormattedCharSequence label, Rect body, int y, float alpha) {
		int labelWidth = font.width(label);
		int centre = body.x() + body.width() / 2;
		int labelLeft = centre - labelWidth / 2;
		int ruleY = y + 4;
		int rule = Gfx.alpha(DIVIDER_RULE, alpha);
		if (labelLeft - 4 > body.x() + 4) {
			graphics.fill(body.x() + 4, ruleY, labelLeft - 4, ruleY + 1, rule);
			graphics.fill(labelLeft + labelWidth + 4, ruleY, body.right() - 4, ruleY + 1, rule);
		}
		graphics.text(font, label, labelLeft, y + 1, Gfx.textAlpha(Theme.DIM_TEXT, alpha), false);
	}

	/** A thin thumb on the right while scrolled back, so you can tell you are not at the bottom. */
	private static void renderScrollHint(GuiGraphicsExtractor graphics, Rect body, int total, int scroll) {
		int rows = Math.max(1, body.height() / Theme.LINE_HEIGHT);
		if (total <= rows) {
			return;
		}
		int height = body.height() - 4;
		int thumb = Math.max(6, height * rows / total);
		int maxScroll = total - rows;
		int top = body.y() + 2 + (height - thumb) * (maxScroll - Math.min(scroll, maxScroll)) / maxScroll;
		graphics.fill(body.right() - 2, body.y() + 2, body.right() - 1, body.bottom() - 2, Theme.SCROLL_TRACK);
		graphics.fill(body.right() - 2, top, body.right() - 1, top + thumb, Theme.SCROLL_THUMB);
	}

	// ---- cursor ---------------------------------------------------------------------------------

	private static void requestCursor(GuiGraphicsExtractor graphics, List<ChatWindow> windows, int mouseX, int mouseY) {
		if (Menu.contains(mouseX, mouseY)) {
			graphics.requestCursor(CursorTypes.POINTING_HAND);
			return;
		}
		if (WindowInput.isDragging()) {
			graphics.requestCursor(WindowInput.isResizing() ? resizeCursor(WindowInput.resizeEdge()) : CursorTypes.RESIZE_ALL);
			return;
		}
		ChatWindow window = WindowInput.windowAt(mouseX, mouseY);
		if (window == null) {
			return;
		}
		Rect bounds = Frame.boundsOf(window);
		var settingsCursor = Settings.cursorAt(window, Frame.body(bounds), mouseX, mouseY);
		if (settingsCursor != null) {
			graphics.requestCursor(settingsCursor);
			return;
		}
		if (TabBar.at(window, bounds, mouseX, mouseY) != null) {
			graphics.requestCursor(CursorTypes.POINTING_HAND);
			return;
		}
		switch (Frame.zoneAt(bounds, mouseX, mouseY, WindowInput.canResize(window))) {
			case MENU_BUTTON -> graphics.requestCursor(CursorTypes.POINTING_HAND);
			case RESIZE -> graphics.requestCursor(resizeCursor(Frame.edgeAt(bounds, mouseX, mouseY)));
			default -> {
			}
		}
	}

	private static com.mojang.blaze3d.platform.cursor.CursorType resizeCursor(Frame.Edge edge) {
		return edge != null && edge.horizontal() ? CursorTypes.RESIZE_EW : CursorTypes.RESIZE_NS;
	}
}
