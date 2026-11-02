package com.gradeaudit.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SessionRendererTest {

	private static final CodeSnippet CODE = new CodeSnippet("int __total__ = 0;\nreturn __total__;", Map.of("total", "sum"));

	private static final Session SESSION = new Session("Priya Nair", "Sum of even numbers", List.of(
			new Event.Prompt("Write a method that sums the even numbers."),
			new Event.AiReply("Here is a first version.", CODE),
			new Event.AiCodeApplied(12),
			new Event.TestRun(3, 5),
			new Event.ManualEdit(1)));

	@Test
	void standardStyle() {
		String expected = """
				CANDIDATE: Priya Nair
				TASK: Sum of even numbers

				[1] CANDIDATE PROMPT: Write a method that sums the even numbers.
				[2] AI REPLY: Here is a first version.
				    CODE:
				    int sum = 0;
				    return sum;
				[3] CODE APPLIED: 12 lines of AI code added to the solution
				[4] TEST RUN: 3 of 5 tests passed
				[5] MANUAL EDIT: the candidate edited 1 line by hand""";

		assertEquals(expected, SessionRenderer.render(SESSION, RenderStyle.STANDARD));
	}

	@Test
	void alternateStyle() {
		String expected = """
				Candidate - Priya Nair
				Task - Sum of even numbers

				Step 1) Candidate prompt -> Write a method that sums the even numbers.

				Step 2) AI reply -> Here is a first version.
				---
				int sum = 0;
				return sum;
				---

				Step 3) Code applied -> 12 lines of AI code added to the solution

				Step 4) Test run -> 3 of 5 tests passed

				Step 5) Manual edit -> the candidate edited 1 line by hand""";

		assertEquals(expected, SessionRenderer.render(SESSION, RenderStyle.ALTERNATE));
	}

	@Test
	void defaultStyleIsStandard() {
		assertEquals(SessionRenderer.render(SESSION, RenderStyle.STANDARD), SessionRenderer.render(SESSION));
	}

	@Test
	void sameSessionAndStyleAlwaysGiveTheSameText() {
		assertEquals(SessionRenderer.render(SESSION), SessionRenderer.render(SESSION));
	}

	@Test
	void theTwoStylesDiffer() {
		assertFalse(SessionRenderer.render(SESSION, RenderStyle.STANDARD)
				.equals(SessionRenderer.render(SESSION, RenderStyle.ALTERNATE)));
	}

	@Test
	void linesAreSeparatedByASingleNewlineAndNothingFollowsTheLastLine() {
		for (RenderStyle style : RenderStyle.values()) {
			String text = SessionRenderer.render(SESSION, style);

			assertFalse(text.contains("\r"));
			assertFalse(text.endsWith("\n"));
		}
	}

	@Test
	void singularAndPluralWording() {
		var session = new Session("Name", "Task", List.of(
				new Event.AiCodeApplied(1),
				new Event.TestRun(1, 1),
				new Event.TestRun(0, 2)));

		String text = SessionRenderer.render(session);

		assertEquals("""
				CANDIDATE: Name
				TASK: Task

				[1] CODE APPLIED: 1 line of AI code added to the solution
				[2] TEST RUN: 1 of 1 test passed
				[3] TEST RUN: 0 of 2 tests passed""", text);
	}

	@Test
	void everyKindOfEventIsRenderedInBothStyles() {
		for (RenderStyle style : RenderStyle.values()) {
			String text = SessionRenderer.render(SESSION, style);

			for (String part : List.of("Write a method", "Here is a first version.", "int sum = 0;", "12 lines",
					"3 of 5 tests passed", "edited 1 line")) {
				assertEquals(true, text.contains(part), style + " is missing: " + part);
			}
		}
	}

	@Test
	void missingArgumentsAreRejected() {
		assertThrows(NullPointerException.class, () -> SessionRenderer.render(null));
		assertThrows(NullPointerException.class, () -> SessionRenderer.render(SESSION, null));
	}

}