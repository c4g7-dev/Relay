package com.c4g7.relay.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * A small, forgiving tokenizer for {@link java.util.regex.Pattern} syntax, used only to colour the
 * pattern in the regex editor. It never throws: unbalanced or unfinished constructs simply end up
 * as plain tokens, while the real validation comes from compiling the pattern.
 */
public final class RegexSyntax {
	public enum Kind {
		LITERAL,
		ESCAPE,
		CLASS,
		GROUP,
		FLAGS,
		QUANTIFIER,
		ANCHOR,
		ALTERNATION,
		DOT,
		QUOTED
	}

	/** A coloured run {@code [start, end)}; {@code depth} is the group nesting level for brackets. */
	public record Token(int start, int end, Kind kind, int depth) {
	}

	private RegexSyntax() {
	}

	public static List<Token> tokenize(String pattern) {
		List<Token> tokens = new ArrayList<>();
		if (pattern == null) {
			return tokens;
		}
		int length = pattern.length();
		int depth = 0;
		int index = 0;
		while (index < length) {
			char current = pattern.charAt(index);
			switch (current) {
				case '\\' -> {
					int end = escapeEnd(pattern, index);
					if (end - index >= 2 && pattern.charAt(index + 1) == 'Q') {
						int close = pattern.indexOf("\\E", end);
						int quoteEnd = close < 0 ? length : close + 2;
						tokens.add(new Token(index, quoteEnd, Kind.QUOTED, depth));
						index = quoteEnd;
					} else {
						tokens.add(new Token(index, end, isAnchorEscape(pattern, index) ? Kind.ANCHOR : Kind.ESCAPE, depth));
						index = end;
					}
				}
				case '[' -> {
					int end = classEnd(pattern, index);
					tokens.add(new Token(index, end, Kind.CLASS, depth));
					index = end;
				}
				case '(' -> {
					int end = groupOpenEnd(pattern, index);
					if (isFlagGroup(pattern, index, end)) {
						tokens.add(new Token(index, end, Kind.FLAGS, depth));
					} else {
						tokens.add(new Token(index, end, Kind.GROUP, depth));
						depth++;
					}
					index = end;
				}
				case ')' -> {
					depth = Math.max(0, depth - 1);
					tokens.add(new Token(index, index + 1, Kind.GROUP, depth));
					index++;
				}
				case '*', '+', '?' -> {
					int end = quantifierSuffixEnd(pattern, index + 1);
					tokens.add(new Token(index, end, Kind.QUANTIFIER, depth));
					index = end;
				}
				case '{' -> {
					int close = boundedQuantifierEnd(pattern, index);
					if (close > 0) {
						int end = quantifierSuffixEnd(pattern, close);
						tokens.add(new Token(index, end, Kind.QUANTIFIER, depth));
						index = end;
					} else {
						tokens.add(new Token(index, index + 1, Kind.LITERAL, depth));
						index++;
					}
				}
				case '^', '$' -> {
					tokens.add(new Token(index, index + 1, Kind.ANCHOR, depth));
					index++;
				}
				case '|' -> {
					tokens.add(new Token(index, index + 1, Kind.ALTERNATION, depth));
					index++;
				}
				case '.' -> {
					tokens.add(new Token(index, index + 1, Kind.DOT, depth));
					index++;
				}
				default -> {
					int end = index + 1;
					while (end < length && isPlain(pattern.charAt(end))) {
						end++;
					}
					tokens.add(new Token(index, end, Kind.LITERAL, depth));
					index = end;
				}
			}
		}
		return tokens;
	}

	/** Index of the bracket matching the one at {@code index}, or -1. Handles () and []. */
	public static int matchingBracket(String pattern, int index) {
		if (pattern == null || index < 0 || index >= pattern.length()) {
			return -1;
		}
		for (Token token : tokenize(pattern)) {
			if (token.kind() == Kind.CLASS && (token.start() == index || token.end() - 1 == index)) {
				boolean closed = token.end() - token.start() >= 2 && pattern.charAt(token.end() - 1) == ']';
				if (!closed) {
					return -1;
				}
				return token.start() == index ? token.end() - 1 : token.start();
			}
		}
		char bracket = pattern.charAt(index);
		if (bracket != '(' && bracket != ')' || isEscaped(pattern, index) || insideClass(pattern, index)) {
			return -1;
		}
		List<Integer> stack = new ArrayList<>();
		for (Token token : tokenize(pattern)) {
			if (token.kind() != Kind.GROUP && token.kind() != Kind.FLAGS) {
				continue;
			}
			char first = pattern.charAt(token.start());
			if (first == '(' && token.kind() == Kind.GROUP) {
				stack.add(token.start());
			} else if (first == ')' && !stack.isEmpty()) {
				int open = stack.remove(stack.size() - 1);
				if (open == index) {
					return token.start();
				}
				if (token.start() == index) {
					return open;
				}
			}
		}
		return -1;
	}

	/** Quotes {@code literal} so it matches itself, preferring readable backslash escapes over \Q..\E. */
	public static String escape(String literal) {
		StringBuilder escaped = new StringBuilder(literal.length() + 8);
		for (int index = 0; index < literal.length(); index++) {
			char character = literal.charAt(index);
			if ("\\^$.|?*+()[]{}".indexOf(character) >= 0) {
				escaped.append('\\');
			}
			escaped.append(character);
		}
		return escaped.toString();
	}

	private static boolean isPlain(char character) {
		return "\\[](){}*+?^$|.".indexOf(character) < 0;
	}

	private static int escapeEnd(String pattern, int index) {
		int length = pattern.length();
		if (index + 1 >= length) {
			return length;
		}
		char next = pattern.charAt(index + 1);
		switch (next) {
			case 'p', 'P', 'k', 'N' -> {
				if (index + 2 < length && (pattern.charAt(index + 2) == '{' || pattern.charAt(index + 2) == '<')) {
					char close = pattern.charAt(index + 2) == '{' ? '}' : '>';
					int end = pattern.indexOf(close, index + 3);
					return end < 0 ? length : end + 1;
				}
				return Math.min(length, index + 3);
			}
			case 'x' -> {
				if (index + 2 < length && pattern.charAt(index + 2) == '{') {
					int end = pattern.indexOf('}', index + 3);
					return end < 0 ? length : end + 1;
				}
				return Math.min(length, index + 4);
			}
			case 'u' -> {
				return Math.min(length, index + 6);
			}
			case 'c' -> {
				return Math.min(length, index + 3);
			}
			case '0' -> {
				int end = index + 2;
				while (end < length && end < index + 5 && pattern.charAt(end) >= '0' && pattern.charAt(end) <= '7') {
					end++;
				}
				return end;
			}
			default -> {
				return index + 2;
			}
		}
	}

	private static boolean isAnchorEscape(String pattern, int index) {
		return index + 1 < pattern.length() && "bBAzZG".indexOf(pattern.charAt(index + 1)) >= 0;
	}

	private static int classEnd(String pattern, int index) {
		int length = pattern.length();
		int cursor = index + 1;
		if (cursor < length && pattern.charAt(cursor) == '^') {
			cursor++;
		}
		if (cursor < length && pattern.charAt(cursor) == ']') {
			cursor++;
		}
		int nested = 0;
		while (cursor < length) {
			char character = pattern.charAt(cursor);
			if (character == '\\') {
				cursor = escapeEnd(pattern, cursor);
				continue;
			}
			if (character == '[') {
				nested++;
			} else if (character == ']') {
				if (nested == 0) {
					return cursor + 1;
				}
				nested--;
			}
			cursor++;
		}
		return length;
	}

	private static int groupOpenEnd(String pattern, int index) {
		int length = pattern.length();
		if (index + 1 >= length || pattern.charAt(index + 1) != '?') {
			return index + 1;
		}
		int cursor = index + 2;
		if (cursor >= length) {
			return length;
		}
		char kind = pattern.charAt(cursor);
		if (kind == ':' || kind == '=' || kind == '!' || kind == '>') {
			return cursor + 1;
		}
		if (kind == '<') {
			if (cursor + 1 < length && (pattern.charAt(cursor + 1) == '=' || pattern.charAt(cursor + 1) == '!')) {
				return cursor + 2;
			}
			int close = pattern.indexOf('>', cursor);
			return close < 0 ? length : close + 1;
		}
		while (cursor < length && (Character.isLetter(pattern.charAt(cursor)) || pattern.charAt(cursor) == '-')) {
			cursor++;
		}
		if (cursor < length && (pattern.charAt(cursor) == ')' || pattern.charAt(cursor) == ':')) {
			return cursor + 1;
		}
		return cursor;
	}

	private static boolean isFlagGroup(String pattern, int start, int end) {
		return end - start >= 3 && pattern.charAt(start + 1) == '?' && pattern.charAt(end - 1) == ')';
	}

	private static int quantifierSuffixEnd(String pattern, int index) {
		if (index < pattern.length() && (pattern.charAt(index) == '?' || pattern.charAt(index) == '+')) {
			return index + 1;
		}
		return index;
	}

	private static int boundedQuantifierEnd(String pattern, int index) {
		int cursor = index + 1;
		int length = pattern.length();
		boolean digits = false;
		while (cursor < length && Character.isDigit(pattern.charAt(cursor))) {
			cursor++;
			digits = true;
		}
		if (cursor < length && pattern.charAt(cursor) == ',') {
			cursor++;
			while (cursor < length && Character.isDigit(pattern.charAt(cursor))) {
				cursor++;
			}
		}
		return digits && cursor < length && pattern.charAt(cursor) == '}' ? cursor + 1 : -1;
	}

	private static boolean isEscaped(String pattern, int index) {
		int backslashes = 0;
		for (int cursor = index - 1; cursor >= 0 && pattern.charAt(cursor) == '\\'; cursor--) {
			backslashes++;
		}
		return backslashes % 2 == 1;
	}

	private static boolean insideClass(String pattern, int index) {
		for (Token token : tokenize(pattern)) {
			if (token.kind() == Kind.CLASS && index > token.start() && index < token.end() - 1) {
				return true;
			}
		}
		return false;
	}
}
