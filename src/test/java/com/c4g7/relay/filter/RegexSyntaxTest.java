package com.c4g7.relay.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class RegexSyntaxTest {
	@Test
	void tokensCoverThePatternWithoutGaps() {
		String pattern = "(?is).*\\[ℹ\\]\\s*⌞Conduit⌝\\s*ᐅ\\s*Service\\s+.+?\\s+\\([^)]+\\)\\s+(?:is\\s+(?:starting|online)(?:…|\\.{3}|!)|did\\s+not).*";
		List<RegexSyntax.Token> tokens = RegexSyntax.tokenize(pattern);
		int expected = 0;
		for (RegexSyntax.Token token : tokens) {
			assertEquals(expected, token.start(), "gap before " + token);
			assertTrue(token.end() > token.start());
			expected = token.end();
		}
		assertEquals(pattern.length(), expected);
		assertEquals(RegexSyntax.Kind.FLAGS, tokens.getFirst().kind());
	}

	@Test
	void classifiesTheCommonPieces() {
		List<RegexSyntax.Token> tokens = RegexSyntax.tokenize("^a+?[b-d]\\d{2,3}|.$");
		assertEquals(RegexSyntax.Kind.ANCHOR, tokens.get(0).kind());
		assertEquals(RegexSyntax.Kind.LITERAL, tokens.get(1).kind());
		assertEquals(RegexSyntax.Kind.QUANTIFIER, tokens.get(2).kind());
		assertEquals(4, tokens.get(2).end());
		assertEquals(RegexSyntax.Kind.CLASS, tokens.get(3).kind());
		assertEquals(RegexSyntax.Kind.ESCAPE, tokens.get(4).kind());
		assertEquals(RegexSyntax.Kind.QUANTIFIER, tokens.get(5).kind());
		assertEquals(RegexSyntax.Kind.ALTERNATION, tokens.get(6).kind());
		assertEquals(RegexSyntax.Kind.DOT, tokens.get(7).kind());
	}

	@Test
	void findsMatchingBrackets() {
		String pattern = "(a(b)c)[)]";
		assertEquals(6, RegexSyntax.matchingBracket(pattern, 0));
		assertEquals(0, RegexSyntax.matchingBracket(pattern, 6));
		assertEquals(4, RegexSyntax.matchingBracket(pattern, 2));
		assertEquals(9, RegexSyntax.matchingBracket(pattern, 7));
		assertEquals(-1, RegexSyntax.matchingBracket(pattern, 8));
	}

	@Test
	void escapedTextMatchesItself() {
		String literal = "[Lobby] (1/20) cost: $5.00 + tax?";
		assertTrue(Pattern.compile(RegexSyntax.escape(literal)).matcher(literal).matches());
	}
}
