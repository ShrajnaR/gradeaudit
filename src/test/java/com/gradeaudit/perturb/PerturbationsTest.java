package com.gradeaudit.perturb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gradeaudit.sample.CandidateNames;
import com.gradeaudit.sample.CodeSnippet;
import com.gradeaudit.sample.Event;
import com.gradeaudit.sample.GeneratedSample;
import com.gradeaudit.sample.Phrases;
import com.gradeaudit.sample.RenderStyle;
import com.gradeaudit.sample.Session;
import com.gradeaudit.sample.SessionGenerator;
import com.gradeaudit.sample.SessionRenderer;
import com.gradeaudit.sample.Template;

class PerturbationsTest {

	private static final CodeSnippet CODE = new CodeSnippet("int __total__ = 0;\nreturn __total__;",
			Map.of("total", "sum"));

	private static final Session SESSION = new Session("Priya Nair", "Sum of even numbers", List.of(
			new Event.Prompt("Write a method that sums the even numbers."),
			new Event.AiReply("Here is a first version.", CODE),
			new Event.AiCodeApplied(12),
			new Event.TestRun(3, 5),
			new Event.ManualEdit(1)));

	private static final List<String> POLITE_PREFIXES = List.of("Hello! ", "Hi there, ", "Could you please help me? ");
	private static final List<String> POLITE_SUFFIXES = List.of("Thank you!", "Thanks a lot.", "Many thanks.");

	private static Session variantOf(PerturbationType type, Session session) {
		return Perturbations.apply(type, session, 42, 0).orElseThrow().session();
	}

	// ---- P1: rename variables -------------------------------------------

	@Test
	void p1_addsValueToEveryVariableNameAndChangesNothingElse() {
		Session changed = variantOf(PerturbationType.P1_RENAME_VARIABLES, SESSION);

		Event.AiReply reply = (Event.AiReply) changed.events().get(1);
		assertEquals("int sumValue = 0;\nreturn sumValue;", reply.code().render());
		assertEquals(SESSION.events().get(0), changed.events().get(0));
		assertEquals(SESSION.events().subList(2, 5), changed.events().subList(2, 5));
		assertEquals(SESSION.candidateName(), changed.candidateName());
	}

	@Test
	void p1_neverMakesTwoDifferentNamesEqual() {
		var code = new CodeSnippet("__a__ + __b__", Map.of("a", "x", "b", "xValue"));
		var session = new Session("N", "T", List.of(new Event.Prompt("hello"), new Event.AiReply("ok", code)));

		Event.AiReply reply = (Event.AiReply) variantOf(PerturbationType.P1_RENAME_VARIABLES, session).events().get(1);

		assertEquals("xValue + xValueValue", reply.code().render());
	}

	@Test
	void p1_isNotApplicableWhenThereIsNoCode() {
		var session = new Session("N", "T", List.of(new Event.Prompt("hello"), new Event.TestRun(1, 2)));

		assertTrue(Perturbations.apply(PerturbationType.P1_RENAME_VARIABLES, session, 1, 0).isEmpty());
	}

	// ---- P2: typos --------------------------------------------------------

	@Test
	void p2_onlyTouchesPrompts() {
		Session changed = variantOf(PerturbationType.P2_TYPOS, SESSION);

		assertEquals(SESSION.events().subList(1, 5), changed.events().subList(1, 5));
		assertEquals(SESSION.candidateName(), changed.candidateName());
		assertNotEquals(SESSION.events().get(0), changed.events().get(0));
	}

	@Test
	void p2_onlySwapsTwoNeighbouringLettersInsideAWord() {
		for (int index = 0; index < 20; index++) {
			Session changed = Perturbations.apply(PerturbationType.P2_TYPOS, SESSION, 9, index).orElseThrow().session();
			String before = ((Event.Prompt) SESSION.events().get(0)).text();
			String after = ((Event.Prompt) changed.events().get(0)).text();

			assertEquals(before.length(), after.length());
			char[] sortedBefore = before.toCharArray();
			char[] sortedAfter = after.toCharArray();
			Arrays.sort(sortedBefore);
			Arrays.sort(sortedAfter);
			assertTrue(Arrays.equals(sortedBefore, sortedAfter), "letters were not just rearranged");

			for (int i = 0; i < before.length() - 1; i++) {
				if (before.charAt(i) != after.charAt(i)) {
					assertEquals(before.charAt(i), after.charAt(i + 1), "not a neighbouring swap at " + i);
					assertEquals(before.charAt(i + 1), after.charAt(i));
					assertTrue(Character.isLetter(before.charAt(i - 1)), "swap touched a first letter");
					assertTrue(Character.isLetter(before.charAt(i + 2)), "swap touched a last letter");
					i++;
				}
			}
		}
	}

	@Test
	void p2_changesAtLeastOnePromptWheneverAnyWordCanBeSwapped() {
		for (int index = 0; index < 50; index++) {
			assertTrue(Perturbations.apply(PerturbationType.P2_TYPOS, SESSION, 3, index).isPresent());
		}
	}

	@Test
	void p2_isNotApplicableWhenNoWordCanBeSwapped() {
		var session = new Session("N", "T", List.of(new Event.Prompt("fix it"), new Event.Prompt("do it")));

		assertTrue(Perturbations.apply(PerturbationType.P2_TYPOS, session, 1, 0).isEmpty());
	}

	@Test
	void p2_doesNotTouchWordsWithRepeatedLettersOnlyOrShortWords() {
		var session = new Session("N", "T", List.of(new Event.Prompt("aaaa bbbb cat")));

		assertTrue(Perturbations.apply(PerturbationType.P2_TYPOS, session, 1, 0).isEmpty());
	}

	// ---- P3: polite filler --------------------------------------------------

	@Test
	void p3_wrapsEveryPromptInAGreetingAndThanks() {
		Session changed = variantOf(PerturbationType.P3_POLITE_FILLER, SESSION);
		String original = ((Event.Prompt) SESSION.events().get(0)).text();
		String text = ((Event.Prompt) changed.events().get(0)).text();

		assertTrue(POLITE_PREFIXES.stream().anyMatch(text::startsWith));
		assertTrue(POLITE_SUFFIXES.stream().anyMatch(text::endsWith));
		assertTrue(text.contains(original));
		assertEquals(SESSION.events().subList(1, 5), changed.events().subList(1, 5));
	}

	@Test
	void p3_addsAFullStopToAPromptWithoutAnEndingMark() {
		var session = new Session("N", "T", List.of(new Event.Prompt("fix it")));

		String text = ((Event.Prompt) variantOf(PerturbationType.P3_POLITE_FILLER, session).events().get(0)).text();

		assertTrue(text.contains("fix it. "), text);
	}

	@Test
	void p3_keepsAnExistingEndingMark() {
		var session = new Session("N", "T", List.of(new Event.Prompt("Is it done?")));

		String text = ((Event.Prompt) variantOf(PerturbationType.P3_POLITE_FILLER, session).events().get(0)).text();

		assertTrue(text.contains("Is it done? "), text);
		assertFalse(text.contains("?."));
	}

	// ---- P4: swap the name ---------------------------------------------------

	@Test
	void p4_givesADifferentNameFromThePoolAndChangesNothingElse() {
		for (String name : CandidateNames.POOL) {
			Session original = SESSION.withCandidateName(name);
			for (int index = 0; index < 10; index++) {
				Session changed = Perturbations.apply(PerturbationType.P4_SWAP_NAME, original, 5, index).orElseThrow()
						.session();

				assertNotEquals(name, changed.candidateName());
				assertTrue(CandidateNames.POOL.contains(changed.candidateName()));
				assertEquals(original.events(), changed.events());
				assertEquals(original.taskTitle(), changed.taskTitle());
			}
		}
	}

	@Test
	void p4_alsoWorksForANameOutsideThePool() {
		Session changed = variantOf(PerturbationType.P4_SWAP_NAME, SESSION.withCandidateName("Somebody Else"));

		assertTrue(CandidateNames.POOL.contains(changed.candidateName()));
	}

	// ---- P5: layout ------------------------------------------------------------

	@Test
	void p5_keepsTheSessionAndUsesTheAlternateLayout() {
		Variant variant = Perturbations.apply(PerturbationType.P5_LAYOUT, SESSION, 1, 0).orElseThrow();

		assertEquals(SESSION, variant.session());
		assertEquals(RenderStyle.ALTERNATE, variant.style());
		assertEquals(SessionRenderer.render(SESSION, RenderStyle.ALTERNATE), variant.text());
	}

	// ---- S1: remove the test runs ---------------------------------------------

	@Test
	void s1_removesEveryTestRunAndKeepsTheRestInOrder() {
		Session changed = variantOf(PerturbationType.S1_REMOVE_TEST_RUNS, SESSION);

		assertEquals(List.of(SESSION.events().get(0), SESSION.events().get(1), SESSION.events().get(2),
				SESSION.events().get(4)), changed.events());
	}

	@Test
	void s1_isNotApplicableToASessionWithoutTestRuns() {
		var session = new Session("N", "T", List.of(new Event.Prompt("hello"), new Event.AiCodeApplied(40)));

		assertTrue(Perturbations.apply(PerturbationType.S1_REMOVE_TEST_RUNS, session, 1, 0).isEmpty());
	}

	@Test
	void s1_leavesTheSessionAloneWhenNothingWouldBeLeft() {
		var session = new Session("N", "T", List.of(new Event.TestRun(1, 2)));

		assertTrue(Perturbations.apply(PerturbationType.S1_REMOVE_TEST_RUNS, session, 1, 0).isEmpty());
	}

	// ---- S2: lazy prompts ------------------------------------------------------

	@Test
	void s2_replacesEveryPromptWithALazyOneAndChangesNothingElse() {
		var session = new Session("N", "T", List.of(new Event.Prompt("Write a careful, detailed method."),
				new Event.AiReply("ok", CODE), new Event.Prompt("Two tests fail on empty input, please fix."),
				new Event.TestRun(1, 2)));

		Session changed = variantOf(PerturbationType.S2_LAZY_PROMPTS, session);

		assertEquals(4, changed.events().size());
		assertTrue(Phrases.LAZY_PROMPTS.contains(((Event.Prompt) changed.events().get(0)).text()));
		assertTrue(Phrases.LAZY_PROMPTS.contains(((Event.Prompt) changed.events().get(2)).text()));
		assertEquals(session.events().get(1), changed.events().get(1));
		assertEquals(session.events().get(3), changed.events().get(3));
	}

	// ---- all changes together ----------------------------------------------------

	@Test
	void theOriginalSessionIsNeverChanged() {
		Session copy = new Session(SESSION.candidateName(), SESSION.taskTitle(), new ArrayList<>(SESSION.events()));

		for (PerturbationType type : PerturbationType.values()) {
			Perturbations.apply(type, SESSION, 42, 0);
		}

		assertEquals(copy, SESSION);
	}

	@Test
	void sameInputsAlwaysGiveTheSameVariant() {
		for (PerturbationType type : PerturbationType.values()) {
			assertEquals(Perturbations.apply(type, SESSION, 42, 3), Perturbations.apply(type, SESSION, 42, 3));
		}
	}

	@Test
	void differentSeedsGiveDifferentRandomChanges() {
		int different = 0;
		for (int index = 0; index < 12; index++) {
			Optional<Variant> first = Perturbations.apply(PerturbationType.P2_TYPOS, SESSION, 1, index);
			Optional<Variant> second = Perturbations.apply(PerturbationType.P2_TYPOS, SESSION, 2, index);
			if (!first.equals(second)) {
				different++;
			}
		}
		assertTrue(different >= 6, "only " + different + " of 12 differ between seeds");
	}

	@Test
	void invalidArgumentsAreRejected() {
		assertThrows(NullPointerException.class, () -> Perturbations.apply(null, SESSION, 1, 0));
		assertThrows(NullPointerException.class,
				() -> Perturbations.apply(PerturbationType.P1_RENAME_VARIABLES, null, 1, 0));
		assertThrows(IllegalArgumentException.class,
				() -> Perturbations.apply(PerturbationType.P1_RENAME_VARIABLES, SESSION, 1, -1));
		assertThrows(NullPointerException.class, () -> Perturbations.applyAll(null, SESSION, 1, 0));
	}

	@Test
	void applyAllReturnsTheApplicableChangesInTheFixedOrder() {
		GeneratedSample careful = SessionGenerator.generate(42, 0);
		GeneratedSample blind = SessionGenerator.generate(42, 2);

		assertEquals(PerturbationType.ofKind(PerturbationKind.INVARIANCE),
				Perturbations.applyAll(PerturbationKind.INVARIANCE, careful.session(), 42, 0).stream()
						.map(Variant::type).toList());
		assertEquals(PerturbationType.ofKind(PerturbationKind.SENSITIVITY),
				Perturbations.applyAll(PerturbationKind.SENSITIVITY, careful.session(), 42, 0).stream()
						.map(Variant::type).toList());
		assertEquals(List.of(PerturbationType.S2_LAZY_PROMPTS),
				Perturbations.applyAll(PerturbationKind.SENSITIVITY, blind.session(), 42, 2).stream()
						.map(Variant::type).toList());
	}

	@Test
	void applyAllResultCannotBeChanged() {
		var variants = Perturbations.applyAll(PerturbationKind.INVARIANCE, SESSION, 1, 0);

		assertThrows(UnsupportedOperationException.class, () -> variants.remove(0));
	}

	// ---- across many generated samples ---------------------------------------------

	@Test
	void everyVariantReallyDiffersFromTheOriginalText() {
		for (long seed = 0; seed < 50; seed++) {
			for (GeneratedSample sample : SessionGenerator.generateAll(seed, 12)) {
				String original = SessionRenderer.render(sample.session());
				for (PerturbationType type : PerturbationType.values()) {
					Optional<Variant> variant = Perturbations.apply(type, sample.session(), seed, sample.index());

					variant.ifPresent(found -> assertNotEquals(original, found.text()));
				}
			}
		}
	}

	@Test
	void whichChangesApplyToGeneratedSamples() {
		Set<String> onlyShortPrompts = Set.of("fix it", "do it");
		for (long seed = 0; seed < 50; seed++) {
			for (GeneratedSample sample : SessionGenerator.generateAll(seed, 12)) {
				Session session = sample.session();
				int index = sample.index();
				boolean hasTestRun = session.events().stream().anyMatch(event -> event instanceof Event.TestRun);
				boolean hasLongerPrompt = session.events().stream().anyMatch(event -> event instanceof Event.Prompt prompt
						&& !onlyShortPrompts.contains(prompt.text()));

				assertTrue(Perturbations.apply(PerturbationType.P1_RENAME_VARIABLES, session, seed, index).isPresent());
				assertEquals(hasLongerPrompt,
						Perturbations.apply(PerturbationType.P2_TYPOS, session, seed, index).isPresent());
				assertTrue(Perturbations.apply(PerturbationType.P3_POLITE_FILLER, session, seed, index).isPresent());
				assertTrue(Perturbations.apply(PerturbationType.P4_SWAP_NAME, session, seed, index).isPresent());
				assertTrue(Perturbations.apply(PerturbationType.P5_LAYOUT, session, seed, index).isPresent());
				assertEquals(hasTestRun,
						Perturbations.apply(PerturbationType.S1_REMOVE_TEST_RUNS, session, seed, index).isPresent());
				if (sample.template() == Template.CAREFUL_VERIFIER || sample.template() == Template.MIXED) {
					assertTrue(Perturbations.apply(PerturbationType.S2_LAZY_PROMPTS, session, seed, index).isPresent());
				}
			}
		}
	}

	/**
	 * A fingerprint of every variant of 36 generated samples. It fails if any change
	 * ever produces different text for the same seed, for example because of a
	 * different Java version or an unordered collection. If the changes or the
	 * wording are altered on purpose, replace the expected value.
	 */
	@Test
	void theVariantsOfGeneratedSamplesHaveNotChanged() throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		for (long seed = 0; seed < 3; seed++) {
			for (GeneratedSample sample : SessionGenerator.generateAll(seed, 12)) {
				for (PerturbationType type : PerturbationType.values()) {
					String text = Perturbations.apply(type, sample.session(), seed, sample.index())
							.map(Variant::text).orElse("NOT APPLICABLE");
					digest.update((seed + "/" + sample.index() + "/" + type.code() + "/" + text + "\0")
							.getBytes(StandardCharsets.UTF_8));
				}
			}
		}

		assertEquals("de6d4344943405b7d99754fb0b6e25098b50914a816396606451c267cb72c44a", HexFormat.of().formatHex(digest.digest()));
	}

}