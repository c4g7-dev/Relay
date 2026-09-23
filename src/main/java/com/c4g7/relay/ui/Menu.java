package com.c4g7.relay.ui;

import com.c4g7.relay.Relay;
import com.c4g7.relay.chat.ChatHub;
import com.c4g7.relay.chat.ChatTab;
import com.c4g7.relay.chat.ChatWindow;
import com.c4g7.relay.util.Rect;
import com.c4g7.relay.util.Tr;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** The ≡ menu of a window: new tab, new window, settings, lock / unlock and delete. */
public final class Menu {
	private static final int ITEM_HEIGHT = 12;
	private static final int MIN_WIDTH = 90;
	private static final int ICON = 10;
	private static final int LABEL_INSET = 17;

	private record Item(String label, Identifier icon, Consumer<ChatWindow> action) {
	}

	@Nullable
	private static ChatWindow owner;
	private static Rect bounds = new Rect(0, 0, 0, 0);
	private static final Tween OPENING = new Tween(0.0F, Theme.MENU_OPEN);
	private static final Map<Integer, Tween> HOVERS = new HashMap<>();

	private Menu() {
	}

	public static boolean isOpen() {
		return owner != null;
	}

	public static void close() {
		owner = null;
	}

	public static void toggle(ChatWindow window, Rect windowBounds) {
		if (owner == window) {
			owner = null;
			return;
		}
		owner = window;
		OPENING.snap(0.0F);
		OPENING.target(1.0F, System.currentTimeMillis());
		HOVERS.clear();
		List<Item> items = items(window);
		int width = widthOf(items);
		int x = Math.max(0, windowBounds.right() - width);
		int y = windowBounds.y() + Theme.TAB_BAR_HEIGHT;
		bounds = new Rect(x, y, x + width, y + items.size() * ITEM_HEIGHT + 2);
	}

	private static int widthOf(List<Item> items) {
		Font font = Minecraft.getInstance().font;
		int widest = 0;
		for (Item item : items) {
			widest = Math.max(widest, font.width(item.label()));
		}
		return Math.max(MIN_WIDTH, LABEL_INSET + widest + 6);
	}

	public static void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY) {
		ChatWindow window = owner;
		if (window == null) {
			return;
		}
		if (!Relay.hub().windows().contains(window)) {
			owner = null;
			return;
		}
		long now = System.currentTimeMillis();
		List<Item> items = items(window);
		float openness = OPENING.value(now);
		int fullHeight = items.size() * ITEM_HEIGHT + 2;
		int bottom = bounds.y() + Math.max(1, Math.round(fullHeight * openness));
		Gfx.rounded(graphics, bounds.x(), bounds.y(), bounds.right(), bottom, 3, Theme.MENU);
		for (int index = 0; index < items.size(); index++) {
			int top = bounds.y() + 1 + index * ITEM_HEIGHT;
			if (top + ITEM_HEIGHT + 1 > bottom) {
				break;
			}
			Item item = items.get(index);
			boolean hovered = mouseY >= top && mouseY < top + ITEM_HEIGHT && mouseX >= bounds.x() && mouseX < bounds.right();
			Tween hover = HOVERS.computeIfAbsent(index, key -> new Tween(0.0F, Theme.MENU_HOVER_FADE));
			float lit = hover.value(now);
			hover.target(hovered ? 1.0F : 0.0F, now);
			if (lit > 0.0F) {
				Gfx.rounded(graphics, bounds.x() + 1, top, bounds.right() - 1, top + ITEM_HEIGHT, 3, Gfx.alpha(Theme.MENU_HOVER, lit));
			}
			Icons.draw(graphics, item.icon(), new Rect(bounds.x() + 3, top + 1, bounds.x() + 3 + ICON, top + 1 + ICON), Gfx.alpha(Theme.TEXT, openness));
			graphics.text(font, item.label(), bounds.x() + LABEL_INSET, top + 3, Gfx.alpha(Theme.TEXT, openness), false);
		}
	}

	public static boolean contains(double mouseX, double mouseY) {
		return owner != null && bounds.contains(mouseX, mouseY);
	}

	public static boolean mouseClicked(double mouseX, double mouseY) {
		if (owner == null) {
			return false;
		}
		if (!bounds.contains(mouseX, mouseY)) {
			owner = null;
			return false;
		}
		int index = (int) ((mouseY - bounds.y() - 1) / ITEM_HEIGHT);
		List<Item> items = items(owner);
		if (index >= 0 && index < items.size()) {
			ChatWindow window = owner;
			owner = null;
			items.get(index).action().accept(window);
		}
		return true;
	}

	private static List<Item> items(ChatWindow window) {
		ChatHub hub = Relay.hub();
		List<Item> items = new ArrayList<>();
		items.add(new Item(Tr.get("relaychat.menu.new_tab", "New tab"), Icons.PLUS, target -> {
			ChatTab tab = hub.createTab(target);
			target.activate(tab);
			Settings.openTab(target, tab);
		}));
		items.add(new Item(Tr.get("relaychat.menu.new_window", "New window"), Icons.PLUS, target -> hub.createWindow()));
		items.add(new Item(Tr.get("relaychat.menu.settings", "Settings"), Icons.GEAR, target -> Settings.openTab(target, target.activeTab())));
		boolean locked = window.config().locked;
		items.add(new Item(locked ? Tr.get("relaychat.menu.unlock", "Unlock position") : Tr.get("relaychat.menu.lock", "Lock position"),
			locked ? Icons.UNLOCK : Icons.LOCK, target -> {
				target.config().locked = !target.config().locked;
				hub.save();
			}));
		if (hub.canDelete(window)) {
			items.add(new Item(Tr.get("relaychat.menu.delete", "Delete"), Icons.BIN, target -> {
				Settings.close(target);
				hub.deleteWindow(target);
			}));
		}
		return items;
	}
}
