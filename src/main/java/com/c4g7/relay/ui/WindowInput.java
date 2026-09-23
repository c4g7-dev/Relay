package com.c4g7.relay.ui;

import com.c4g7.relay.Relay;
import com.c4g7.relay.chat.ChatHub;
import com.c4g7.relay.chat.ChatTab;
import com.c4g7.relay.chat.ChatWindow;
import com.c4g7.relay.config.Style;
import com.c4g7.relay.util.Rect;
import com.c4g7.relay.util.Tr;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** Mouse handling for the windows: tabs, menu, moving, resizing, scrolling, copying and clicking text. */
public final class WindowInput {
	/** Lets the chat screen run a clicked text component (open URL, run or suggest a command, ...). */
	@FunctionalInterface
	public interface ComponentClicker {
		boolean click(net.minecraft.network.chat.Style style, boolean insert);
	}

	private enum Gesture {
		NONE,
		MOVE,
		RESIZE
	}

	private static final int LEFT = 0;
	private static final int RIGHT = 1;
	private static final int SCROLL_LINES = 3;

	private static Gesture gesture = Gesture.NONE;
	@Nullable
	private static ChatWindow gestureWindow;
	private static Frame.Edge edge;
	private static double grabX;
	private static double grabY;
	@Nullable
	private static Rect working;

	private WindowInput() {
	}

	private static boolean enabled() {
		ChatHub hub = Relay.hub();
		return hub != null && hub.config().behaviour.enabled;
	}

	public static boolean canMove(ChatWindow window) {
		return Relay.hub().config().behaviour.draggable && !window.config().locked;
	}

	public static boolean canResize(ChatWindow window) {
		return Relay.hub().config().behaviour.resizable && !window.config().locked;
	}

	/** The topmost window under the point, including its 3 px grab margin. */
	@Nullable
	public static ChatWindow windowAt(double x, double y) {
		List<ChatWindow> windows = new ArrayList<>(Relay.hub().windows());
		Collections.reverse(windows);
		for (ChatWindow window : windows) {
			if (window.config().visibility.drawnWhile(true)
				&& Frame.zoneAt(Frame.boundsOf(window), x, y, canResize(window)) != Frame.Zone.OUTSIDE) {
				return window;
			}
		}
		return null;
	}

	public static boolean mouseClicked(double x, double y, int button, boolean shift, ComponentClicker clicker) {
		if (!enabled()) {
			return false;
		}
		if (Menu.mouseClicked(x, y)) {
			return true;
		}
		ChatWindow window = windowAt(x, y);
		if (window == null) {
			Settings.commitAll();
			return false;
		}
		if (button == RIGHT) {
			return copyMessageAt(window, x, y);
		}
		if (button != LEFT) {
			return false;
		}
		Rect bounds = Frame.boundsOf(window);
		Frame.Zone zone = Frame.zoneAt(bounds, x, y, canResize(window));
		if (zone == Frame.Zone.BODY && Settings.mouseClicked(window, Frame.body(bounds), x, y, shift)) {
			return true;
		}
		Settings.commitAll();
		ChatTab clickedTab = TabBar.at(window, bounds, x, y);
		if (clickedTab != null) {
			switchTo(window, clickedTab);
			return true;
		}
		switch (zone) {
			case MENU_BUTTON -> {
				Menu.toggle(window, bounds);
				return true;
			}
			case TAB_BAR -> {
				return startMove(window, bounds, x, y);
			}
			case BODY -> {
				if (clickText(window, bounds, x, y, shift, clicker)) {
					return true;
				}
				return startMove(window, bounds, x, y);
			}
			case RESIZE -> {
				gesture = Gesture.RESIZE;
				gestureWindow = window;
				edge = Frame.edgeAt(bounds, x, y);
				working = bounds;
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	private static boolean startMove(ChatWindow window, Rect bounds, double x, double y) {
		if (!canMove(window)) {
			// A locked window still swallows the click so it never reaches the game underneath.
			return true;
		}
		gesture = Gesture.MOVE;
		gestureWindow = window;
		working = bounds;
		grabX = x - bounds.x();
		grabY = y - bounds.y();
		return true;
	}

	public static void switchTo(ChatWindow window, ChatTab tab) {
		if (window.activeTab() != tab) {
			Settings.close(window);
		}
		window.activate(tab);
		Relay.hub().save();
	}

	public static boolean mouseDragged(double x, double y) {
		if (!enabled()) {
			return false;
		}
		for (ChatWindow window : Relay.hub().windows()) {
			if (Settings.mouseDragged(window, Frame.body(Frame.boundsOf(window)), x)) {
				return true;
			}
		}
		if (gesture == Gesture.NONE || gestureWindow == null || working == null) {
			return false;
		}
		Rect screen = Frame.screen();
		if (gesture == Gesture.RESIZE) {
			if (edge != null) {
				working = Frame.resized(working, edge, x, y, screen);
			}
		} else {
			working = Frame.moved(working, x - grabX, y - grabY, screen);
		}
		gestureWindow.config().geometry.store(working, screen.width(), screen.height());
		return true;
	}

	public static boolean mouseReleased() {
		if (!enabled()) {
			return false;
		}
		boolean handled = false;
		for (ChatWindow window : Relay.hub().windows()) {
			handled |= Settings.mouseReleased(window);
		}
		if (gesture == Gesture.NONE) {
			return handled;
		}
		gesture = Gesture.NONE;
		gestureWindow = null;
		edge = null;
		working = null;
		Relay.hub().save();
		return true;
	}

	public static boolean mouseScrolled(double x, double y, double amount, boolean shift) {
		if (!enabled()) {
			return false;
		}
		ChatWindow window = windowAt(x, y);
		if (window == null) {
			return false;
		}
		Rect body = Frame.body(Frame.boundsOf(window));
		if (Settings.isOpen(window)) {
			return Settings.mouseScrolled(window, body, amount);
		}
		window.scroll(window.scroll() + (int) Math.signum(amount) * (shift ? 1 : SCROLL_LINES));
		return true;
	}

	private static boolean copyMessageAt(ChatWindow window, double x, double y) {
		if (Settings.isOpen(window)) {
			return false;
		}
		Lines.Line line = lineAt(window, Frame.boundsOf(window), y);
		if (line == null) {
			return false;
		}
		String text = Lines.copyText(line.entry(), window.activeTab().config().style(Relay.hub().config()));
		if (text.isEmpty()) {
			return false;
		}
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.keyboardHandler.setClipboard(text);
		minecraft.gui.hud.setOverlayMessage(Component.literal(Tr.get("relaychat.message.copied", "Message copied")), false);
		return true;
	}

	private static boolean clickText(ChatWindow window, Rect bounds, double x, double y, boolean shift, ComponentClicker clicker) {
		ChatTab tab = window.activeTab();
		if (tab == null) {
			return false;
		}
		Rect body = Frame.body(bounds);
		Style style = tab.config().style(Relay.hub().config());
		Font font = Minecraft.getInstance().font;
		int indent = style.marker ? 4 : 0;
		int textLeft = body.x() + Theme.PADDING + indent;
		List<Lines.Line> lines = Lines.of(tab, font, body.width() - 2 * Theme.PADDING - indent, style, true);
		int index = Lines.at(body, lines.size(), window.scroll(), y);
		if (index < 0 || lines.get(index).entry().message().isDivider()) {
			return false;
		}
		int[] range = Lines.window(body, lines.size(), window.scroll());
		int lineY = body.bottom() - Theme.LINE_HEIGHT * (range[1] - index);
		ActiveTextCollector.ClickableStyleFinder finder = new ActiveTextCollector.ClickableStyleFinder(font, (int) x, (int) y).includeInsertions(shift);
		finder.accept(TextAlignment.LEFT, textLeft, lineY, lines.get(index).text());
		net.minecraft.network.chat.Style clicked = finder.result();
		return clicked != null && clicker.click(clicked, shift);
	}

	private static Lines.Line lineAt(ChatWindow window, Rect bounds, double y) {
		ChatTab tab = window.activeTab();
		if (tab == null) {
			return null;
		}
		Rect body = Frame.body(bounds);
		Style style = tab.config().style(Relay.hub().config());
		int indent = style.marker ? 4 : 0;
		List<Lines.Line> lines = Lines.of(tab, Minecraft.getInstance().font, body.width() - 2 * Theme.PADDING - indent, style, true);
		int index = Lines.at(body, lines.size(), window.scroll(), y);
		return index < 0 ? null : lines.get(index);
	}

	public static Frame.Edge highlightedEdge(ChatWindow window, Rect bounds, double x, double y) {
		if (gesture == Gesture.NONE) {
			if (windowAt(x, y) != window || TabBar.at(window, bounds, x, y) != null || !canResize(window)) {
				return null;
			}
			return Frame.zoneAt(bounds, x, y, true) == Frame.Zone.RESIZE ? Frame.edgeAt(bounds, x, y) : null;
		}
		return window == gestureWindow && gesture == Gesture.RESIZE ? edge : null;
	}

	public static boolean isResizing(ChatWindow window) {
		return gesture == Gesture.RESIZE && window == gestureWindow;
	}

	public static boolean isDragging() {
		return gesture != Gesture.NONE;
	}

	public static boolean isResizing() {
		return gesture == Gesture.RESIZE;
	}

	public static Frame.Edge resizeEdge() {
		return edge;
	}
}
