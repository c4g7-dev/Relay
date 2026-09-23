package com.c4g7.relay.ui.text;

/** Counts rapid clicks on the same spot: 1 = place caret, 2 = select word, 3 = select all. */
public final class Clicks {
	private static final long WINDOW_MILLIS = 300L;
	private static final double SLOP = 3.0;

	private long lastAt;
	private double lastX;
	private double lastY;
	private int count;

	public int register(double x, double y) {
		long now = System.currentTimeMillis();
		if (now - this.lastAt <= WINDOW_MILLIS && Math.abs(x - this.lastX) <= SLOP && Math.abs(y - this.lastY) <= SLOP) {
			this.count = this.count >= 3 ? 1 : this.count + 1;
		} else {
			this.count = 1;
		}
		this.lastAt = now;
		this.lastX = x;
		this.lastY = y;
		return this.count;
	}
}
