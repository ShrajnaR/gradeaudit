package com.gradeaudit.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class EventTest {

	private static final CodeSnippet CODE = new CodeSnippet("return 1;", Map.of());

	@Test
	void validEventsAreCreated() {
		assertEquals("hello", new Event.Prompt("hello").text());
		assertEquals("done", new Event.AiReply("done", CODE).explanation());
		assertEquals(12, new Event.AiCodeApplied(12).lines());
		assertEquals(3, new Event.ManualEdit(3).lines());
		assertEquals(4, new Event.TestRun(4, 5).passed());
	}

	@Test
	void promptNeedsText() {
		assertThrows(IllegalArgumentException.class, () -> new Event.Prompt(null));
		assertThrows(IllegalArgumentException.class, () -> new Event.Prompt(""));
		assertThrows(IllegalArgumentException.class, () -> new Event.Prompt("   "));
	}

	@Test
	void aiReplyNeedsExplanationAndCode() {
		assertThrows(IllegalArgumentException.class, () -> new Event.AiReply(" ", CODE));
		assertThrows(NullPointerException.class, () -> new Event.AiReply("ok", null));
	}

	@Test
	void appliedAndEditedLinesMustBePositive() {
		assertThrows(IllegalArgumentException.class, () -> new Event.AiCodeApplied(0));
		assertThrows(IllegalArgumentException.class, () -> new Event.AiCodeApplied(-1));
		assertThrows(IllegalArgumentException.class, () -> new Event.ManualEdit(0));
		assertEquals(1, new Event.ManualEdit(1).lines());
	}

	@Test
	void testRunCountsMustBeConsistent() {
		assertThrows(IllegalArgumentException.class, () -> new Event.TestRun(0, 0));
		assertThrows(IllegalArgumentException.class, () -> new Event.TestRun(-1, 5));
		assertThrows(IllegalArgumentException.class, () -> new Event.TestRun(6, 5));
		assertEquals(0, new Event.TestRun(0, 5).passed());
	}

	@Test
	void allPassedOnlyWhenEveryTestPassed() {
		assertTrue(new Event.TestRun(5, 5).allPassed());
		assertFalse(new Event.TestRun(4, 5).allPassed());
		assertFalse(new Event.TestRun(0, 5).allPassed());
	}

	@Test
	void eventsWithEqualValuesAreEqual() {
		assertEquals(new Event.TestRun(3, 5), new Event.TestRun(3, 5));
		assertEquals(new Event.Prompt("a"), new Event.Prompt("a"));
	}

}