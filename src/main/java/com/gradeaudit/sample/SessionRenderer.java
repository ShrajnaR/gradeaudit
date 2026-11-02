package com.gradeaudit.sample;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Writes a {@link Session} out as the text that is sent to a grader.
 *
 * <p>The output is fully determined by the session and the style. Lines are
 * always joined with a single newline character, never the operating system's
 * own separator, so the same session gives the same text on every computer.
 * There is no newline after the last line.
 */
public final class SessionRenderer {

	private static final String CODE_INDENT = "    ";

	private SessionRenderer() {
	}

	/** Renders in the standard style. */
	public static String render(Session session) {
		return render(session, RenderStyle.STANDARD);
	}

	public static String render(Session session, RenderStyle style) {
		Objects.requireNonNull(session, "session");
		Objects.requireNonNull(style, "style");
		return switch (style) {
			case STANDARD -> renderStandard(session);
			case ALTERNATE -> renderAlternate(session);
		};
	}

	private static String renderStandard(Session session) {
		List<String> lines = new ArrayList<>();
		lines.add("CANDIDATE: " + session.candidateName());
		lines.add("TASK: " + session.taskTitle());
		lines.add("");

		int number = 1;
		for (Event event : session.events()) {
			Entry entry = describe(event);
			lines.add("[" + number + "] " + entry.standardLabel() + ": " + entry.sentence());
			if (!entry.codeLines().isEmpty()) {
				lines.add(CODE_INDENT + "CODE:");
				for (String codeLine : entry.codeLines()) {
					lines.add(codeLine.isEmpty() ? "" : CODE_INDENT + codeLine);
				}
			}
			number++;
		}
		return String.join("\n", lines);
	}

	private static String renderAlternate(Session session) {
		List<String> blocks = new ArrayList<>();
		blocks.add("Candidate - " + session.candidateName() + "\nTask - " + session.taskTitle());

		int number = 1;
		for (Event event : session.events()) {
			Entry entry = describe(event);
			List<String> block = new ArrayList<>();
			block.add("Step " + number + ") " + entry.alternateLabel() + " -> " + entry.sentence());
			if (!entry.codeLines().isEmpty()) {
				block.add("---");
				block.addAll(entry.codeLines());
				block.add("---");
			}
			blocks.add(String.join("\n", block));
			number++;
		}
		return String.join("\n\n", blocks);
	}

	/** What one event says, independent of the style it is written in. */
	private static Entry describe(Event event) {
		if (event instanceof Event.Prompt prompt) {
			return new Entry("CANDIDATE PROMPT", "Candidate prompt", prompt.text(), List.of());
		}
		if (event instanceof Event.AiReply reply) {
			return new Entry("AI REPLY", "AI reply", reply.explanation(), reply.code().render().lines().toList());
		}
		if (event instanceof Event.AiCodeApplied applied) {
			return new Entry("CODE APPLIED", "Code applied",
					count(applied.lines(), "line") + " of AI code added to the solution", List.of());
		}
		if (event instanceof Event.ManualEdit edit) {
			return new Entry("MANUAL EDIT", "Manual edit",
					"the candidate edited " + count(edit.lines(), "line") + " by hand", List.of());
		}
		if (event instanceof Event.TestRun run) {
			return new Entry("TEST RUN", "Test run",
					run.passed() + " of " + count(run.total(), "test") + " passed", List.of());
		}
		throw new IllegalStateException("Unknown event type: " + event.getClass().getName());
	}

	private static String count(int number, String noun) {
		return number + " " + (number == 1 ? noun : noun + "s");
	}

	private record Entry(String standardLabel, String alternateLabel, String sentence, List<String> codeLines) {
	}

}