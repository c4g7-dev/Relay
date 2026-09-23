package com.c4g7.relay.ui.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TextBufferTest {
	@Test
	void typingReplacesTheSelection() {
		TextBuffer buffer = new TextBuffer("hello world", 256);
		buffer.select(0, 5);
		buffer.type("H");
		assertEquals("H world", buffer.text());
		assertEquals(1, buffer.caret());
		assertFalse(buffer.hasSelection());
	}

	@Test
	void shiftArrowsGrowAndShrinkTheSelection() {
		TextBuffer buffer = new TextBuffer("abcdef", 256);
		buffer.home(false);
		buffer.right(true, false);
		buffer.right(true, false);
		assertEquals("ab", buffer.selectedText());
		buffer.left(true, false);
		assertEquals("a", buffer.selectedText());
		buffer.right(false, false);
		assertFalse(buffer.hasSelection());
		assertEquals(1, buffer.caret());
	}

	@Test
	void wordJumpsSkipWhitespaceAndStopAtPunctuation() {
		TextBuffer buffer = new TextBuffer("foo bar.baz  qux", 256);
		buffer.home(false);
		buffer.right(false, true);
		assertEquals(4, buffer.caret());
		buffer.right(false, true);
		assertEquals(7, buffer.caret());
		buffer.end(false);
		buffer.left(false, true);
		assertEquals(13, buffer.caret());
		buffer.left(false, true);
		assertEquals(8, buffer.caret());
	}

	@Test
	void ctrlBackspaceDeletesTheWordBeforeTheCaret() {
		TextBuffer buffer = new TextBuffer("one two three", 256);
		buffer.backspace(true);
		assertEquals("one two ", buffer.text());
		buffer.backspace(true);
		assertEquals("one ", buffer.text());
	}

	@Test
	void selectAllCutAndUndo() {
		TextBuffer buffer = new TextBuffer("regex", 256);
		buffer.selectAll();
		assertEquals("regex", buffer.cut());
		assertEquals("", buffer.text());
		assertTrue(buffer.undo());
		assertEquals("regex", buffer.text());
		assertTrue(buffer.redo());
		assertEquals("", buffer.text());
	}

	@Test
	void typingMergesIntoOneUndoStepUntilASpace() {
		TextBuffer buffer = new TextBuffer("", 256);
		for (char character : "abc def".toCharArray()) {
			buffer.type(String.valueOf(character));
		}
		assertEquals("abc def", buffer.text());
		buffer.undo();
		assertEquals("abc ", buffer.text());
		buffer.undo();
		assertEquals("", buffer.text());
	}

	@Test
	void doubleClickSelectsTheWholeWord() {
		TextBuffer buffer = new TextBuffer("[Conduit] Service lobby-1 is online", 256);
		buffer.selectWordAt(12);
		assertEquals("Service", buffer.selectedText());
		buffer.selectWordAt(20);
		assertEquals("lobby", buffer.selectedText());
	}

	@Test
	void wrapSurroundsTheSelection() {
		TextBuffer buffer = new TextBuffer("online|offline", 256);
		buffer.selectAll();
		buffer.wrap("(?:", ")");
		assertEquals("(?:online|offline)", buffer.text());
		assertEquals("online|offline", buffer.selectedText());
	}

	@Test
	void respectsTheMaximumLength() {
		TextBuffer buffer = new TextBuffer("1234", 6);
		buffer.insert("56789");
		assertEquals("123456", buffer.text());
		buffer.select(0, 2);
		buffer.insert("abc");
		assertEquals("ab3456", buffer.text());
	}
}
