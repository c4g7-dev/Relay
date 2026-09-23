package com.c4g7.relay.config;

import com.c4g7.relay.util.Rect;

/**
 * Where a window sits. The position is stored relative to the nearest screen edge as a fraction of
 * the screen, so windows keep their place when the GUI scale or the game window size changes.
 */
public final class Geometry {
	public enum Horizontal {
		LEFT,
		CENTER,
		RIGHT
	}

	public enum Vertical {
		TOP,
		CENTER,
		BOTTOM
	}

	public Horizontal horizontal = Horizontal.LEFT;
	public Vertical vertical = Vertical.BOTTOM;
	/** Distance from the anchored edge, as a fraction (0..1) of the screen width / height. */
	public float x = 0.0F;
	public float y = 0.0F;
	public int width = 300;
	public int height = 150;

	public Geometry() {
	}

	public Geometry(Horizontal horizontal, Vertical vertical, float x, float y, int width, int height) {
		this.horizontal = horizontal;
		this.vertical = vertical;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	/** The window's rectangle on a screen of the given size, before it is confined to the usable area. */
	public Rect resolve(int screenWidth, int screenHeight) {
		int left = switch (this.horizontal) {
			case LEFT -> Math.round(this.x * screenWidth);
			case RIGHT -> Math.round(screenWidth - this.x * screenWidth) - this.width;
			case CENTER -> (screenWidth - this.width) / 2;
		};
		int top = switch (this.vertical) {
			case TOP -> Math.round(this.y * screenHeight);
			case BOTTOM -> Math.round(screenHeight - this.y * screenHeight) - this.height;
			case CENTER -> (screenHeight - this.height) / 2;
		};
		return new Rect(left, top, left + this.width, top + this.height);
	}

	/** Stores {@code bounds} anchored to whichever screen edges its centre is closest to. */
	public void store(Rect bounds, int screenWidth, int screenHeight) {
		this.width = bounds.width();
		this.height = bounds.height();
		float half = screenWidth / 2.0F;
		if (bounds.centerX() <= half) {
			this.horizontal = Horizontal.LEFT;
			this.x = fraction(bounds.x(), screenWidth);
		} else {
			this.horizontal = Horizontal.RIGHT;
			this.x = fraction(screenWidth - bounds.right(), screenWidth);
		}
		if (bounds.centerY() <= screenHeight / 2.0F) {
			this.vertical = Vertical.TOP;
			this.y = fraction(bounds.y(), screenHeight);
		} else {
			this.vertical = Vertical.BOTTOM;
			this.y = fraction(screenHeight - bounds.bottom(), screenHeight);
		}
	}

	private static float fraction(int distance, int size) {
		return size <= 0 ? 0.0F : Math.max(0.0F, Math.min(1.0F, distance / (float) size));
	}

	void sanitize() {
		if (this.horizontal == null) {
			this.horizontal = Horizontal.LEFT;
		}
		if (this.vertical == null) {
			this.vertical = Vertical.BOTTOM;
		}
		this.x = Float.isFinite(this.x) ? Math.max(0.0F, Math.min(1.0F, this.x)) : 0.0F;
		this.y = Float.isFinite(this.y) ? Math.max(0.0F, Math.min(1.0F, this.y)) : 0.0F;
		this.width = Math.max(120, this.width);
		this.height = Math.max(40, this.height);
	}
}
