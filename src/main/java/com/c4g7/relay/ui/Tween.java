package com.c4g7.relay.ui;

/** A float that eases (out-cubic) towards a target over a fixed duration. */
public final class Tween {
	private final long duration;
	private float from;
	private float to;
	private long startedAt = Long.MIN_VALUE;

	public Tween(float initial, long duration) {
		this.duration = Math.max(1L, duration);
		this.from = initial;
		this.to = initial;
	}

	public float value(long now) {
		if (this.startedAt == Long.MIN_VALUE || this.from == this.to) {
			return this.to;
		}
		long elapsed = now - this.startedAt;
		if (elapsed >= this.duration) {
			this.from = this.to;
			return this.to;
		}
		if (elapsed <= 0L) {
			return this.from;
		}
		float t = elapsed / (float) this.duration;
		float inverted = 1.0F - t;
		return this.from + (this.to - this.from) * (1.0F - inverted * inverted * inverted);
	}

	public void target(float target, long now) {
		if (target != this.to) {
			this.from = this.value(now);
			this.to = target;
			this.startedAt = now;
		}
	}

	public void snap(float value) {
		this.from = value;
		this.to = value;
		this.startedAt = Long.MIN_VALUE;
	}
}
