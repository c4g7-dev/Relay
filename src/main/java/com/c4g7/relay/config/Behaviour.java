package com.c4g7.relay.config;

/** Settings that apply to Relay as a whole rather than to one tab. */
public final class Behaviour {
	public boolean enabled = true;
	public boolean draggable = true;
	public boolean resizable = true;
	/** Keep every tab's messages when you leave a server, join another one or the proxy switches you. */
	public boolean keepAcrossServers = true;
	/** Puts a thin "server" line into the tabs whenever you land on a different server. */
	public boolean serverDivider = true;
	/** Saves the tabs' recent messages on exit and brings them back on the next join. */
	public boolean restoreAfterRestart = false;
	/** How many messages per tab "restore after restart" keeps. */
	public int restoreLimit = 100;

	void sanitize() {
		this.restoreLimit = Math.max(10, Math.min(this.restoreLimit, 1000));
	}
}
