package com.c4g7.relay.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.c4g7.relay.config.FilterConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

class FilterMatcherTest {
	private static FilterConfig words(String... include) {
		FilterConfig filter = new FilterConfig();
		filter.include = List.of(include);
		return filter;
	}

	private static FilterConfig regex(String include, FilterConfig.RegexMatch match) {
		FilterConfig filter = new FilterConfig();
		filter.mode = FilterConfig.Mode.REGEX;
		filter.includeRegex = include;
		filter.regexMatch = match;
		return filter;
	}

	@Test
	void wordsAreCaseInsensitiveUnlessAskedOtherwise() {
		FilterConfig filter = words("lobby");
		assertTrue(FilterMatcher.matches(filter, "Welcome to the LOBBY", () -> ""));
		filter.caseSensitive = true;
		assertFalse(FilterMatcher.matches(filter, "Welcome to the LOBBY", () -> ""));
	}

	@Test
	void excludedWordsWin() {
		FilterConfig filter = words("joined");
		filter.exclude = List.of("bot");
		assertTrue(FilterMatcher.matches(filter, "Steve joined", () -> ""));
		assertFalse(FilterMatcher.matches(filter, "ModBot joined", () -> ""));
	}

	@Test
	void tooltipsAreOnlySearchedWhenEnabled() {
		FilterConfig filter = words("secret");
		assertFalse(FilterMatcher.matches(filter, "hover me", () -> "{\"hover_event\":\"secret\"}"));
		filter.searchJson = true;
		assertTrue(FilterMatcher.matches(filter, "hover me", () -> "{\"hover_event\":\"secret\"}"));
	}

	@Test
	void containsModeFindsAnywhereWholeModeNeedsEverything() {
		assertTrue(FilterMatcher.matches(regex("is (online|offline)", FilterConfig.RegexMatch.CONTAINS), "Service lobby is online!", () -> ""));
		assertFalse(FilterMatcher.matches(regex("is (online|offline)", FilterConfig.RegexMatch.WHOLE), "Service lobby is online!", () -> ""));
		assertTrue(FilterMatcher.matches(regex(".*is (online|offline).*", FilterConfig.RegexMatch.WHOLE), "Service lobby is online!", () -> ""));
	}

	@Test
	void caseSensitivityIsNoLongerInverted() {
		FilterConfig filter = regex("hello", FilterConfig.RegexMatch.CONTAINS);
		assertTrue(FilterMatcher.matches(filter, "HELLO there", () -> ""));
		filter.caseSensitive = true;
		assertFalse(FilterMatcher.matches(filter, "HELLO there", () -> ""));
	}

	@Test
	void excludeRegexVetoesAndInvalidPatternsNeverMatch() {
		FilterConfig filter = regex("joined", FilterConfig.RegexMatch.CONTAINS);
		filter.excludeRegex = "^\\[Bot]";
		assertTrue(FilterMatcher.matches(filter, "Alex joined", () -> ""));
		assertFalse(FilterMatcher.matches(filter, "[Bot] Alex joined", () -> ""));
		filter.includeRegex = "(unclosed";
		assertFalse(FilterMatcher.matches(filter, "(unclosed", () -> ""));
		FilterMatcher.Compiled compiled = FilterMatcher.compile("(unclosed", false);
		assertFalse(compiled.valid());
		assertEquals(9, compiled.errorIndex());
	}

	@Test
	void conduitLifecycleFilterStillWorks() {
		FilterConfig filter = regex(
			"(?is).*\\[ℹ\\]\\s*⌞Conduit⌝\\s*ᐅ\\s*Service\\s+.+?\\s+\\([^)]+\\)\\s+(?:is\\s+(?:starting|online|stopping|stopped|deleted|rebuilding|deleting\\s+its\\s+files)(?:…|\\.{3}|!)|did\\s+not\\s+start:\\s*.*).*",
			FilterConfig.RegexMatch.WHOLE);
		assertTrue(FilterMatcher.matches(filter, "[ℹ] ⌞Conduit⌝ ᐅ Service lobby-1 (Paper 26.2) is online!", () -> ""));
		assertTrue(FilterMatcher.matches(filter, "[ℹ] ⌞Conduit⌝ ᐅ Service bw-3 (Paper) did not start: port in use", () -> ""));
		assertFalse(FilterMatcher.matches(filter, "[ℹ] ⌞Conduit⌝ ᐅ Backup finished", () -> ""));
	}

	@Test
	void spansListEveryMatch() {
		List<int[]> spans = FilterMatcher.spans(FilterMatcher.compile("o+", false).pattern(), "foo boo", 10);
		assertEquals(2, spans.size());
		assertEquals(1, spans.get(0)[0]);
		assertEquals(3, spans.get(0)[1]);
	}

	@Test
	void runawayPatternsGiveUpInsteadOfFreezing() {
		// Polynomial blow-up that modern JDKs do not optimise away: minutes without a time limit.
		java.util.regex.Pattern evil = FilterMatcher.compile("(?:.*,){12}X", true).pattern();
		String input = "a,".repeat(40);
		long started = System.nanoTime();
		org.junit.jupiter.api.Assertions.assertNull(FilterMatcher.test(FilterConfig.RegexMatch.CONTAINS, evil, input, 30_000_000L));
		assertTrue(System.nanoTime() - started < 1_000_000_000L);
		assertFalse(FilterMatcher.test(FilterConfig.RegexMatch.CONTAINS, evil, input));
	}
}
