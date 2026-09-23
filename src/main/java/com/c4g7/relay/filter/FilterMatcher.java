package com.c4g7.relay.filter;

import com.c4g7.relay.config.FilterConfig;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Decides whether a message matches a filter. Compiled patterns are cached, so matching stays cheap. */
public final class FilterMatcher {
	private static final int CACHE_SIZE = 128;
	/** Time one pattern may spend on one message before it counts as not matching. */
	public static final long MESSAGE_BUDGET_NANOS = 20_000_000L;
	private static final Map<String, Compiled> CACHE = new LinkedHashMap<>(CACHE_SIZE, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Compiled> eldest) {
			return this.size() > CACHE_SIZE;
		}
	};

	/** A compiled pattern, or the reason it did not compile (with the character index of the problem). */
	public record Compiled(Pattern pattern, String error, int errorIndex) {
		public boolean valid() {
			return this.pattern != null;
		}
	}

	private FilterMatcher() {
	}

	/**
	 * @param plain the message as the player sees it
	 * @param json the message's serialised JSON, only computed when the filter searches tooltips
	 */
	public static boolean matches(FilterConfig filter, String plain, Supplier<String> json) {
		if (filter.regex()) {
			return matchesRegex(filter, filter.includeRegex, filter.excludeRegex, plain, json);
		}
		boolean included = false;
		for (String word : filter.include) {
			if (containsWord(filter, plain, word) || filter.searchJson && containsWord(filter, json.get(), word)) {
				included = true;
				break;
			}
		}
		if (!included) {
			return false;
		}
		for (String word : filter.exclude) {
			if (containsWord(filter, plain, word) || filter.searchJson && containsWord(filter, json.get(), word)) {
				return false;
			}
		}
		return true;
	}

	/** Regex matching with explicit patterns, so the regex editor can preview edits before they are saved. */
	public static boolean matchesRegex(FilterConfig filter, String include, String exclude, String plain, Supplier<String> json) {
		Compiled included = compile(include, filter.caseSensitive);
		if (!included.valid() || include.isEmpty() || !test(filter, included.pattern(), plain, json)) {
			return false;
		}
		if (exclude == null || exclude.isEmpty()) {
			return true;
		}
		Compiled excluded = compile(exclude, filter.caseSensitive);
		return !excluded.valid() || !test(filter, excluded.pattern(), plain, json);
	}

	private static boolean test(FilterConfig filter, Pattern pattern, String plain, Supplier<String> json) {
		if (test(filter.regexMatch, pattern, plain)) {
			return true;
		}
		return filter.searchJson && test(filter.regexMatch, pattern, json.get());
	}

	public static boolean test(FilterConfig.RegexMatch mode, Pattern pattern, String text) {
		Boolean result = test(mode, pattern, text, MESSAGE_BUDGET_NANOS);
		return result != null && result;
	}

	/** @return whether it matched, or null when the pattern ran out of time on this text */
	public static Boolean test(FilterConfig.RegexMatch mode, Pattern pattern, String text, long budgetNanos) {
		if (text == null) {
			return false;
		}
		try {
			Matcher matcher = pattern.matcher(new TimedText(text, System.nanoTime() + budgetNanos));
			return mode == FilterConfig.RegexMatch.WHOLE ? matcher.matches() : matcher.find();
		} catch (TimedText.OutOfTime slow) {
			return null;
		}
	}

	/** Every {@code [start, end)} span the pattern finds in {@code text}, or null if it ran out of time. */
	public static List<int[]> spans(Pattern pattern, String text, int limit) {
		List<int[]> spans = new ArrayList<>();
		if (pattern == null || text == null) {
			return spans;
		}
		try {
			Matcher matcher = pattern.matcher(new TimedText(text, System.nanoTime() + MESSAGE_BUDGET_NANOS));
			while (spans.size() < limit && matcher.find()) {
				if (matcher.end() > matcher.start()) {
					spans.add(new int[] {matcher.start(), matcher.end()});
				} else if (matcher.end() >= text.length()) {
					break;
				}
			}
		} catch (TimedText.OutOfTime slow) {
			return null;
		}
		return spans;
	}

	/**
	 * Text that gives up once a deadline passes. A badly backtracking pattern (think {@code (?:.*,){12}X})
	 * can otherwise spin for minutes on one chat line and freeze the game; this turns that into a
	 * plain "no match".
	 */
	static final class TimedText implements CharSequence {
		static final class OutOfTime extends RuntimeException {
			OutOfTime() {
				super(null, null, false, false);
			}
		}

		private final CharSequence text;
		private final long deadline;
		private int reads;

		TimedText(CharSequence text, long deadline) {
			this.text = text;
			this.deadline = deadline;
		}

		@Override
		public char charAt(int index) {
			if ((++this.reads & 0x3FF) == 0 && System.nanoTime() > this.deadline) {
				throw new OutOfTime();
			}
			return this.text.charAt(index);
		}

		@Override
		public int length() {
			return this.text.length();
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			return new TimedText(this.text.subSequence(start, end), this.deadline);
		}

		@Override
		public String toString() {
			return this.text.toString();
		}
	}

	public static synchronized Compiled compile(String regex, boolean caseSensitive) {
		String source = regex == null ? "" : regex;
		String key = (caseSensitive ? "1" : "0") + source;
		Compiled cached = CACHE.get(key);
		if (cached != null) {
			return cached;
		}
		Compiled compiled;
		try {
			int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
			compiled = new Compiled(Pattern.compile(source, flags), null, -1);
		} catch (PatternSyntaxException invalid) {
			compiled = new Compiled(null, invalid.getDescription(), invalid.getIndex());
		}
		CACHE.put(key, compiled);
		return compiled;
	}

	private static boolean containsWord(FilterConfig filter, String haystack, String word) {
		if (haystack == null || word == null || word.isEmpty()) {
			return false;
		}
		if (filter.caseSensitive) {
			return haystack.contains(word);
		}
		return haystack.toLowerCase(Locale.ROOT).contains(word.toLowerCase(Locale.ROOT));
	}
}
