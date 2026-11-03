package com.gradeaudit.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gradeaudit.common.Grade;

class SessionGeneratorTest {

	private static final int SEEDS = 100;
	private static final int INDEXES = 12;

	private static final Set<String> CLEAR_PROMPTS = new HashSet<>(Task.ALL.stream().map(Task::clearPrompt).toList());
	private static final Set<String> TASK_TITLES = new HashSet<>(Task.ALL.stream().map(Task::title).toList());

	// ---- determinism and independence -----------------------------------

	@Test
	void sameSeedAndIndexGiveTheSameSample() {
		assertEquals(SessionGenerator.generate(42, 5), SessionGenerator.generate(42, 5));
	}

	@Test
	void differentSeedsGiveDifferentSamples() {
		int different = 0;
		for (int index = 0; index < INDEXES; index++) {
			if (!SessionGenerator.generate(1, index).equals(SessionGenerator.generate(2, index))) {
				different++;
			}
		}
		assertTrue(different >= 10, "only " + different + " of " + INDEXES + " samples differ between seeds");
	}

	@Test
	void differentIndexesInTheSameSeedGiveDifferentSessions() {
		Set<Session> sessions = new HashSet<>();
		for (int index = 0; index < 40; index++) {
			sessions.add(SessionGenerator.generate(7, index).session());
		}
		assertEquals(40, sessions.size());
	}

	@Test
	void aSampleDoesNotDependOnHowManyAreGenerated() {
		List<GeneratedSample> twelve = SessionGenerator.generateAll(7, 12);

		assertEquals(SessionGenerator.generate(7, 9), twelve.get(9));
		assertEquals(twelve.subList(0, 5), SessionGenerator.generateAll(7, 5));
	}

	@Test
	void generateAllReturnsTheRequestedNumberInOrder() {
		List<GeneratedSample> samples = SessionGenerator.generateAll(3, 10);

		assertEquals(10, samples.size());
		for (int index = 0; index < samples.size(); index++) {
			assertEquals(index, samples.get(index).index());
		}
	}

	@Test
	void generatedListCannotBeChanged() {
		List<GeneratedSample> samples = SessionGenerator.generateAll(3, 4);

		assertThrows(UnsupportedOperationException.class, () -> samples.remove(0));
	}

	@Test
	void invalidArgumentsAreRejected() {
		assertThrows(IllegalArgumentException.class, () -> SessionGenerator.generateAll(1, 0));
		assertThrows(IllegalArgumentException.class, () -> SessionGenerator.generateAll(1, -3));
		assertThrows(IllegalArgumentException.class, () -> SessionGenerator.generate(1, -1));
	}

	/**
	 * The exact text of one sample. This fails if the generator ever gives a different
	 * result for the same seed, for example because of a different Java version or
	 * an unordered collection. If the generator or the phrases are changed on
	 * purpose, update this expected text.
	 */
	@Test
	void seed42Sample0ProducesExactlyThisText() {
		String expected = """
				CANDIDATE: Emily Carter
				TASK: Sum of even numbers

				[1] CANDIDATE PROMPT: Write a Java method that takes a list of integers and returns the sum of the even numbers. It must return 0 for an empty list and must not modify the input.
				[2] AI REPLY: Here is an initial implementation.
				    CODE:
				    int total = 0;
				    for (int number : items) {
				        if (number % 2 == 0) {
				            total += number;
				        }
				    }
				    return total;
				[3] CODE APPLIED: 10 lines of AI code added to the solution
				[4] TEST RUN: 4 of 7 tests passed
				[5] CANDIDATE PROMPT: The failing tests are the empty input case and an input with duplicate values. Please correct the method for both and tell me what caused the failures.
				[6] AI REPLY: This revised version fixes the problems you described.
				    CODE:
				    int total = 0;
				    for (int number : items) {
				        if (number % 2 == 0) {
				            total += number;
				        }
				    }
				    return total;
				[7] CODE APPLIED: 3 lines of AI code added to the solution
				[8] TEST RUN: 7 of 7 tests passed
				[9] MANUAL EDIT: the candidate edited 3 lines by hand
				[10] TEST RUN: 7 of 7 tests passed""";

		assertEquals(expected, SessionRenderer.render(SessionGenerator.generate(42, 0).session()));
	}

	// ---- the fixed structure of every sample -----------------------------

	@Test
	void templatesRepeatInOrderWithTheirDesignedGrades() {
		Grade[] expectedGrades = { Grade.A, Grade.B, Grade.C, Grade.C };
		for (int index = 0; index < INDEXES; index++) {
			GeneratedSample sample = SessionGenerator.generate(5, index);

			assertEquals(index, sample.index());
			assertEquals(Template.values()[index % 4], sample.template());
			assertEquals(expectedGrades[index % 4], sample.designedGrade());
		}
	}

	@Test
	void everySampleUsesTheFixedNamesAndTasks() {
		for (GeneratedSample sample : everySample()) {
			assertTrue(CandidateNames.POOL.contains(sample.session().candidateName()));
			assertTrue(TASK_TITLES.contains(sample.session().taskTitle()));
		}
	}

	@Test
	void everySampleRendersInBothStylesWithoutLeftoverPlaceholders() {
		for (GeneratedSample sample : everySample()) {
			for (RenderStyle style : RenderStyle.values()) {
				String text = SessionRenderer.render(sample.session(), style);

				assertTrue(text.contains(sample.session().candidateName()));
				assertTrue(text.contains(sample.session().taskTitle()));
				assertFalse(text.contains("__"), "placeholder left in sample " + sample.index());
			}
		}
	}

	@Test
	void carefulVerifierTestsEveryChangeFailsFirstAndEndsGreen() {
		for (GeneratedSample sample : samplesOf(Template.CAREFUL_VERIFIER)) {
			List<Event> events = sample.session().events();
			List<Event.TestRun> runs = testRuns(events);

			assertEquals(2, appliedCount(events));
			assertTrue(everyApplyIsTestedImmediately(events));
			assertFalse(runs.get(0).allPassed(), "the first test run should fail");
			assertTrue(runs.stream().skip(1).allMatch(Event.TestRun::allPassed));
			assertTrue(events.get(events.size() - 1) instanceof Event.TestRun);
			assertTrue(CLEAR_PROMPTS.contains(prompts(events).get(0)));
			assertTrue(Phrases.SPECIFIC_FOLLOW_UPS.contains(prompts(events).get(1)));
		}
	}

	@Test
	void mixedTestsTheFirstChangeButNotTheSecondStraightAway() {
		for (GeneratedSample sample : samplesOf(Template.MIXED)) {
			List<Event> events = sample.session().events();

			assertEquals(2, appliedCount(events));
			assertEquals(1, appliedImmediatelyTestedCount(events));
			assertFalse(testRuns(events).get(0).allPassed());
			assertTrue(events.get(events.size() - 1) instanceof Event.TestRun last && last.allPassed());
			assertTrue(CLEAR_PROMPTS.contains(prompts(events).get(0)));
			assertTrue(Phrases.MEDIUM_FOLLOW_UPS.contains(prompts(events).get(1)));
		}
	}

	@Test
	void blindPasterNeverRunsTestsAndPastesLargeCodeWithLazyPrompts() {
		for (GeneratedSample sample : samplesOf(Template.BLIND_PASTER)) {
			List<Event> events = sample.session().events();

			assertTrue(testRuns(events).isEmpty());
			assertEquals(3, appliedCount(events));
			assertTrue(appliedLines(events).stream().allMatch(lines -> lines >= 35));
			assertTrue(Phrases.LAZY_PROMPTS.containsAll(prompts(events)));
		}
	}

	@Test
	void ignoresFailuresTestsEveryTimeAndNeverGetsGreen() {
		for (GeneratedSample sample : samplesOf(Template.IGNORES_FAILURES)) {
			List<Event> events = sample.session().events();

			assertEquals(3, testRuns(events).size());
			assertTrue(testRuns(events).stream().noneMatch(Event.TestRun::allPassed));
			assertEquals(3, appliedCount(events));
			assertTrue(everyApplyIsTestedImmediately(events));
			assertTrue(appliedLines(events).stream().allMatch(lines -> lines >= 30));
			assertTrue(CLEAR_PROMPTS.contains(prompts(events).get(0)));
			assertTrue(Phrases.LAZY_PROMPTS.containsAll(prompts(events).subList(1, 3)));
		}
	}

	@Test
	void testRunsAlwaysHaveAtLeastTwoFailuresWhenTheyFail() {
		for (GeneratedSample sample : everySample()) {
			for (Event.TestRun run : testRuns(sample.session().events())) {
				assertTrue(run.total() >= 5 && run.total() <= 8);
				assertTrue(run.allPassed() || run.total() - run.passed() >= 2);
			}
		}
	}

	@Test
	void samplesOfOneBehaviourStillVary() {
		Set<String> tasks = new HashSet<>();
		Set<String> names = new HashSet<>();
		Set<String> codes = new HashSet<>();
		for (GeneratedSample sample : samplesOf(Template.CAREFUL_VERIFIER)) {
			tasks.add(sample.session().taskTitle());
			names.add(sample.session().candidateName());
			codes.add(firstCode(sample.session().events()));
		}
		assertEquals(Task.ALL.size(), tasks.size());
		assertEquals(CandidateNames.POOL.size(), names.size());
		assertTrue(codes.size() > Task.ALL.size(), "variable names should vary within a task");
	}

	@Test
	void theTwoRepliesInASessionShowTheSameCode() {
		for (GeneratedSample sample : everySample()) {
			Set<String> codes = new HashSet<>();
			for (Event event : sample.session().events()) {
				if (event instanceof Event.AiReply reply) {
					codes.add(reply.code().render());
				}
			}
			assertEquals(1, codes.size());
		}
	}

	@Test
	void anAppliedChangeIsNeverSmallerThanTheCodeItCameFrom() {
		for (GeneratedSample sample : samplesOf(Template.CAREFUL_VERIFIER)) {
			List<Event> events = sample.session().events();
			int codeLines = (int) ((Event.AiReply) events.get(1)).code().render().lines().count();

			assertTrue(appliedLines(events).get(0) >= codeLines);
			assertNotEquals(0, codeLines);
		}
	}

	// ---- helpers -------------------------------------------------------

	private static List<GeneratedSample> everySample() {
		List<GeneratedSample> samples = new ArrayList<>();
		for (long seed = 0; seed < SEEDS; seed++) {
			samples.addAll(SessionGenerator.generateAll(seed, INDEXES));
		}
		return samples;
	}

	private static List<GeneratedSample> samplesOf(Template template) {
		return everySample().stream().filter(sample -> sample.template() == template).toList();
	}

	private static List<Event.TestRun> testRuns(List<Event> events) {
		List<Event.TestRun> runs = new ArrayList<>();
		for (Event event : events) {
			if (event instanceof Event.TestRun run) {
				runs.add(run);
			}
		}
		return runs;
	}

	private static List<String> prompts(List<Event> events) {
		List<String> prompts = new ArrayList<>();
		for (Event event : events) {
			if (event instanceof Event.Prompt prompt) {
				prompts.add(prompt.text());
			}
		}
		return prompts;
	}

	private static List<Integer> appliedLines(List<Event> events) {
		List<Integer> lines = new ArrayList<>();
		for (Event event : events) {
			if (event instanceof Event.AiCodeApplied applied) {
				lines.add(applied.lines());
			}
		}
		return lines;
	}

	private static int appliedCount(List<Event> events) {
		return appliedLines(events).size();
	}

	private static int appliedImmediatelyTestedCount(List<Event> events) {
		int count = 0;
		for (int i = 0; i < events.size() - 1; i++) {
			if (events.get(i) instanceof Event.AiCodeApplied && events.get(i + 1) instanceof Event.TestRun) {
				count++;
			}
		}
		return count;
	}

	private static boolean everyApplyIsTestedImmediately(List<Event> events) {
		return appliedImmediatelyTestedCount(events) == appliedCount(events);
	}

	private static String firstCode(List<Event> events) {
		for (Event event : events) {
			if (event instanceof Event.AiReply reply) {
				return reply.code().render();
			}
		}
		throw new IllegalStateException("no AI reply in session");
	}

}