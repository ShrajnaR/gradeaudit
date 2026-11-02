package com.gradeaudit.sample;

import java.util.Objects;

/**
 * One thing that happened during a practice assessment session.
 *
 * <p>There are exactly five kinds, so the type is sealed: no other class can
 * add a sixth kind by accident. Each kind is an immutable record that checks
 * its own values when it is created, so an invalid event can never exist.
 */
public sealed interface Event {

	/** The candidate typed a message to the AI assistant. */
	record Prompt(String text) implements Event {

		public Prompt {
			requireText(text, "text");
		}

	}

	/** The AI assistant answered with an explanation and a piece of code. */
	record AiReply(String explanation, CodeSnippet code) implements Event {

		public AiReply {
			requireText(explanation, "explanation");
			Objects.requireNonNull(code, "code");
		}

	}

	/** The candidate put code from the AI reply into their solution. */
	record AiCodeApplied(int lines) implements Event {

		public AiCodeApplied {
			requirePositive(lines, "lines");
		}

	}

	/** The candidate edited the code by hand. */
	record ManualEdit(int lines) implements Event {

		public ManualEdit {
			requirePositive(lines, "lines");
		}

	}

	/** The candidate ran the tests. */
	record TestRun(int passed, int total) implements Event {

		public TestRun {
			if (total < 1) {
				throw new IllegalArgumentException("total must be at least 1 but was " + total);
			}
			if (passed < 0 || passed > total) {
				throw new IllegalArgumentException("passed must be between 0 and " + total + " but was " + passed);
			}
		}

		/** True if every test passed. */
		public boolean allPassed() {
			return passed == total;
		}

	}

	private static void requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
	}

	private static void requirePositive(int value, String field) {
		if (value < 1) {
			throw new IllegalArgumentException(field + " must be at least 1 but was " + value);
		}
	}

}