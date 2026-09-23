package com.c4g7.relay.ui;

import com.c4g7.relay.chat.ChatTab;
import com.c4g7.relay.chat.ChatWindow;
import com.c4g7.relay.util.Rect;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import org.jspecify.annotations.Nullable;

/** Tab buttons along the top of a window: text width plus 4 px padding on each side, 2 px apart. */
public final class TabBar {
	public record Button(ChatTab tab, String label, int x, int y, int width) {
		boolean contains(double mouseX, double mouseY) {
			return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + Theme.TAB_BAR_HEIGHT;
		}
	}

	private TabBar() {
	}

	public static List<Button> layout(ChatWindow window, Rect bounds, Font font) {
		List<Button> buttons = new ArrayList<>();
		int x = bounds.x() + 2;
		for (ChatTab tab : window.tabs()) {
			String label = tab.title();
			int width = font.width(label) + 8;
			buttons.add(new Button(tab, label, x, bounds.y(), width));
			x += width + 2;
		}
		return buttons;
	}

	/** Right edge available to tabs: the menu button, and the padlock when locked, sit past it. */
	public static int limit(Rect bounds, boolean locked) {
		return bounds.right() - Theme.MENU_BUTTON - (locked ? Theme.MENU_BUTTON : 0);
	}

	@Nullable
	public static ChatTab at(ChatWindow window, Rect bounds, double mouseX, double mouseY) {
		int limit = limit(bounds, window.config().locked);
		for (Button button : layout(window, bounds, net.minecraft.client.Minecraft.getInstance().font)) {
			if (button.x() < limit && mouseX < limit && button.contains(mouseX, mouseY)) {
				return button.tab();
			}
		}
		return null;
	}
}
