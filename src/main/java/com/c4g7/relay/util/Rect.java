package com.c4g7.relay.util;

/** An integer GUI-space rectangle; {@code right} and {@code bottom} are exclusive. */
public record Rect(int x, int y, int right, int bottom) {
	public static Rect of(double x, double y, double right, double bottom) {
		return new Rect((int) x, (int) y, (int) right, (int) bottom);
	}

	public int width() {
		return this.right - this.x;
	}

	public int height() {
		return this.bottom - this.y;
	}

	public float centerX() {
		return (this.x + this.right) / 2.0F;
	}

	public float centerY() {
		return (this.y + this.bottom) / 2.0F;
	}

	public boolean isEmpty() {
		return this.right <= this.x || this.bottom <= this.y;
	}

	public boolean contains(double px, double py) {
		return px >= this.x && px < this.right && py >= this.y && py < this.bottom;
	}

	public Rect inset(int by) {
		return new Rect(this.x + by, this.y + by, this.right - by, this.bottom - by);
	}

	public Rect inset(int horizontal, int vertical) {
		return new Rect(this.x + horizontal, this.y + vertical, this.right - horizontal, this.bottom - vertical);
	}

	public Rect withX(int newX, int newRight) {
		return new Rect(newX, this.y, newRight, this.bottom);
	}

	public Rect withY(int newY, int newBottom) {
		return new Rect(this.x, newY, this.right, newBottom);
	}

	public Rect translate(int dx, int dy) {
		return new Rect(this.x + dx, this.y + dy, this.right + dx, this.bottom + dy);
	}
}
