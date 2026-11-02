package com.gradeaudit.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CodeSnippetTest {

	private static final String TEMPLATE = "int __total__ = 0;\nfor (int __item__ : __items__) {\n    __total__ += __item__;\n}";

	@Test
	void rendersEveryPlaceholderWithItsName() {
		var snippet = new CodeSnippet(TEMPLATE, Map.of("total", "sum", "item", "value", "items", "values"));

		assertEquals("int sum = 0;\nfor (int value : values) {\n    sum += value;\n}", snippet.render());
	}

	@Test
	void sameNameUsedTwiceIsReplacedEverywhere() {
		var snippet = new CodeSnippet("__a__ = __a__ + 1;", Map.of("a", "x"));

		assertEquals("x = x + 1;", snippet.render());
	}

	@Test
	void keysThatStartTheSameDoNotClash() {
		// "item" and "items" must not interfere with each other
		var snippet = new CodeSnippet("__item__ in __items__", Map.of("item", "a", "items", "b"));

		assertEquals("a in b", snippet.render());
	}

	@Test
	void withNamesGivesAnotherSnippetAndLeavesTheOriginalAlone() {
		var original = new CodeSnippet("return __x__;", Map.of("x", "first"));
		var renamed = original.withNames(Map.of("x", "second"));

		assertEquals("return first;", original.render());
		assertEquals("return second;", renamed.render());
	}

	@Test
	void templateWithoutPlaceholdersNeedsNoNames() {
		assertEquals("return 1;", new CodeSnippet("return 1;", Map.of()).render());
	}

	@Test
	void placeholderWithoutANameIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> new CodeSnippet("return __x__;", Map.of()));
	}

	@Test
	void blankTemplateIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> new CodeSnippet(" ", Map.of()));
		assertThrows(IllegalArgumentException.class, () -> new CodeSnippet(null, Map.of()));
	}

	@Test
	void nullNamesAreRejected() {
		assertThrows(NullPointerException.class, () -> new CodeSnippet("return 1;", null));
	}

	@Test
	void namesMustBePlainIdentifiers() {
		assertThrows(IllegalArgumentException.class, () -> new CodeSnippet("__x__", Map.of("x", "bad name")));
		assertThrows(IllegalArgumentException.class, () -> new CodeSnippet("__x__", Map.of("x", "a__b")));
		assertThrows(IllegalArgumentException.class, () -> new CodeSnippet("__x__", Map.of("x", "1abc")));
		assertThrows(IllegalArgumentException.class, () -> new CodeSnippet("__x__", Map.of("x", "")));
		assertThrows(IllegalArgumentException.class, () -> new CodeSnippet("__x__", Map.of("bad key", "y")));
	}

	@Test
	void laterChangesToTheCallersMapDoNotAffectTheSnippet() {
		Map<String, String> names = new HashMap<>();
		names.put("x", "first");
		var snippet = new CodeSnippet("return __x__;", names);

		names.put("x", "changed");

		assertEquals("return first;", snippet.render());
	}

}