package com.c4g7.relay.config;

/** When a window is drawn. */
public enum Visibility {
	ALWAYS("relaychat.visibility.always", "Always"),
	ON_NEW_MESSAGE("relaychat.visibility.on_new_message", "On new message"),
	CHAT_OPEN("relaychat.visibility.chat_open", "Chat open only");

	public final String key;
	public final String english;

	Visibility(String key, String english) {
		this.key = key;
		this.english = english;
	}

	public boolean drawnWhile(boolean chatOpen) {
		return chatOpen || this != CHAT_OPEN;
	}

	/** Whether faded-out messages still show; otherwise lines fade ten seconds after they arrive. */
	public boolean showsEverything(boolean chatOpen) {
		return chatOpen || this == ALWAYS;
	}

	public Visibility next() {
		Visibility[] all = values();
		return all[(this.ordinal() + 1) % all.length];
	}
}
