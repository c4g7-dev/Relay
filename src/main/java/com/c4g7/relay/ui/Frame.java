package com.c4g7.relay.ui;

import com.c4g7.relay.chat.ChatWindow;
import com.c4g7.relay.util.Rect;
import net.minecraft.client.Minecraft;

/** Window geometry: where the tab bar, body and grab edges are, and how moving and resizing clamp. */
public final class Frame {
	public enum Zone {
		OUTSIDE,
		TAB_BAR,
		MENU_BUTTON,
		RESIZE,
		BODY
	}

	public enum Edge {
		LEFT,
		TOP,
		RIGHT,
		BOTTOM;

		public boolean horizontal() {
			return this == LEFT || this == RIGHT;
		}
	}

	private Frame() {
	}

	public static Rect screen() {
		Minecraft minecraft = Minecraft.getInstance();
		return new Rect(0, 0, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
	}

	/** The screen minus the strip the chat input box needs at the bottom. */
	public static Rect usable(Rect screen) {
		return new Rect(screen.x(), screen.y(), screen.right(), Math.max(screen.y() + Theme.MIN_HEIGHT, screen.bottom() - Theme.INPUT_RESERVE));
	}

	public static Rect boundsOf(ChatWindow window) {
		Rect screen = screen();
		return confine(window.config().geometry.resolve(screen.width(), screen.height()), screen);
	}

	public static Rect confine(Rect window, Rect screen) {
		Rect usable = usable(screen);
		int width = Math.min(window.width(), usable.width());
		int height = Math.min(window.height(), usable.height());
		int left = clamp(window.x(), usable.x(), usable.right() - width);
		int top = clamp(window.y(), usable.y(), usable.bottom() - height);
		return new Rect(left, top, left + width, top + height);
	}

	public static Rect body(Rect window) {
		return new Rect(window.x(), window.y() + Theme.TAB_BAR_HEIGHT, window.right(), window.bottom());
	}

	public static Rect menuButton(Rect window) {
		return new Rect(window.right() - Theme.MENU_BUTTON, window.y(), window.right(), window.y() + Theme.TAB_BAR_HEIGHT);
	}

	/** The little padlock shown left of the menu button while a window is locked. */
	public static Rect lockBadge(Rect window) {
		return menuButton(window).translate(-Theme.MENU_BUTTON, 0);
	}

	public static Zone zoneAt(Rect window, double x, double y, boolean resizable) {
		int edge = Theme.EDGE;
		if (x < window.x() - edge || x >= window.right() + edge || y < window.y() - edge || y >= window.bottom() + edge) {
			return Zone.OUTSIDE;
		}
		boolean inside = window.contains(x, y);
		if (inside && menuButton(window).contains(x, y)) {
			return Zone.MENU_BUTTON;
		}
		if (resizable && edgeAt(window, x, y) != null) {
			return Zone.RESIZE;
		}
		if (!inside) {
			return Zone.OUTSIDE;
		}
		return y < window.y() + Theme.TAB_BAR_HEIGHT ? Zone.TAB_BAR : Zone.BODY;
	}

	public static Edge edgeAt(Rect window, double x, double y) {
		int edge = Theme.EDGE;
		boolean rows = y >= window.y() - edge && y < window.bottom() + edge;
		boolean columns = x >= window.x() - edge && x < window.right() + edge;
		if (rows && Math.abs(x - window.x()) <= edge) {
			return Edge.LEFT;
		}
		if (columns && Math.abs(y - window.y()) <= edge) {
			return Edge.TOP;
		}
		if (rows && Math.abs(x - window.right()) <= edge) {
			return Edge.RIGHT;
		}
		if (columns && Math.abs(y - window.bottom()) <= edge) {
			return Edge.BOTTOM;
		}
		return null;
	}

	public static Rect moved(Rect window, double left, double top, Rect screen) {
		Rect usable = usable(screen);
		int x = clamp((int) left, usable.x(), usable.right() - window.width());
		int y = clamp((int) top, usable.y(), usable.bottom() - window.height());
		return new Rect(x, y, x + window.width(), y + window.height());
	}

	public static Rect resized(Rect window, Edge edge, double mouseX, double mouseY, Rect screen) {
		Rect usable = usable(screen);
		int left = window.x();
		int top = window.y();
		int right = window.right();
		int bottom = window.bottom();
		switch (edge) {
			case LEFT -> left = clamp((int) mouseX, usable.x(), right - Theme.MIN_WIDTH);
			case TOP -> top = clamp((int) mouseY, usable.y(), bottom - Theme.MIN_HEIGHT);
			case RIGHT -> right = clamp((int) mouseX, left + Theme.MIN_WIDTH, usable.right());
			case BOTTOM -> bottom = clamp((int) mouseY, top + Theme.MIN_HEIGHT, usable.bottom());
		}
		return new Rect(left, top, right, bottom);
	}

	private static int clamp(int value, int min, int max) {
		return max < min ? min : Math.max(min, Math.min(max, value));
	}
}
