package com.c4g7.relay.ui;

/**
 * Relay's palette and metrics. These reproduce VelvetChat's look one to one (panel tints, row
 * greys, the green/red state colours, the blue accent) so the windows feel exactly the same.
 */
public final class Theme {
	// window
	public static final int WINDOW_BACKGROUND = 0x80000000;
	public static final int TAB_BAR = 0x90101010;
	public static final int ACTIVE_TAB = 0xC0303060;
	public static final int ACCENT = 0xFF5A8CFF;
	public static final int TEXT = 0xFFFFFFFF;
	public static final int INACTIVE_TEXT = 0xFFA0A0A0;
	public static final int SELECTION = 0x805A8CFF;

	// settings panel
	public static final int PANEL = 0xB3000000;
	public static final int HEADER = 0x4D000000;
	public static final int ROW = 0x1EFAFAFA;
	public static final int ROW_HOVER = 0x18FFFFFF;
	public static final int NEUTRAL_BAR = 0x8C6E6E6E;
	public static final int CONTROL = 0x64000000;
	public static final int EDITING = 0x64141414;
	public static final int DIM_TEXT = 0xFFA0A0A8;
	public static final int ON = 0xFF2E8527;
	public static final int OFF = 0xFF851313;
	public static final int CHECKER = 0xFF404048;
	public static final int KNOB_ON = 0xFF368D2F;
	public static final int KNOB_OFF = 0xFF8D2F38;
	public static final int KNOB_MARK = 0xFFCDDECC;
	public static final int SCROLL_TRACK = 0x32646464;
	public static final int SCROLL_THUMB = 0x64C8C8C8;
	public static final int ERROR = 0xFFE05555;

	// popup menu
	public static final int MENU = 0xE0101010;
	public static final int MENU_HOVER = 0x60FFFFFF;

	// colour picker channels (A, R, G, B)
	public static final int[] CHANNELS = {0xFFB0B0B0, 0xFFC03030, 0xFF30C030, 0xFF3030C0};

	// window metrics
	public static final int TAB_BAR_HEIGHT = 14;
	public static final int MENU_BUTTON = 10;
	public static final int EDGE = 3;
	public static final int MIN_WIDTH = 120;
	public static final int MIN_HEIGHT = 40;
	public static final int INPUT_RESERVE = 26;
	public static final int LINE_HEIGHT = 10;
	public static final int PADDING = 2;

	// timings (ms)
	public static final long TAB_SLIDE = 160L;
	public static final long EDGE_FADE = 120L;
	public static final long ROW_HOVER_FADE = 110L;
	public static final long TOGGLE_SLIDE = 130L;
	public static final long MENU_OPEN = 140L;
	public static final long MENU_HOVER_FADE = 90L;
	public static final long VISIBLE = 10_000L;
	public static final long FADE = 1_000L;

	private Theme() {
	}
}
