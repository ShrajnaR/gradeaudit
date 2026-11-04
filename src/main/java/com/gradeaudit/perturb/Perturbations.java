package com.gradeaudit.perturb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.UnaryOperator;

import com.gradeaudit.sample.CandidateNames;
import com.gradeaudit.sample.CodeSnippet;
import com.gradeaudit.sample.Event;
import com.gradeaudit.sample.Phrases;
import com.gradeaudit.sample.RenderStyle;
import com.gradeaudit.sample.Session;
import com.gradeaudit.sample.SessionRenderer;

/**
 * Applies the seven changes to a session.
 *
 * <p>Every change works on the structured session and never edits finished text,
 * so a change cannot damage anything it was not meant to touch. The result is
 * fully determined by the change, the session, the seed and the sample index.
 *
 * <p>A change that would leave the text a grader sees exactly as it was is
 * reported as "not applicable" (an empty result), because grading identical text
 * again would say nothing.
 */
public final class Perturbations {

	/** Keeps these random streams different from the ones the generator uses. */
	private static final long SALT = 0x9E3779B97F4A7C15L;

	private static final List<String> POLITE_PREFIXES = List.of(
			"Hello! ",
			"Hi there, ",
			"Could you please help me? ");

	private static final List<String> POLITE_SUFFIXES = List.of(
			"Thank you!",
			"Thanks a lot.",
			"Many thanks.");

	private Perturbations() {
	}

	/**
	 * One change applied to one session, or empty if the change would not alter
	 * what the grader sees.
	 *
	 * @param seed  the audit's seed
	 * @param index the sample's position in the audit
	 */
	public static Optional<Variant> apply(PerturbationType type, Session original, long seed, int index) {
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(original, "original");
		if (index < 0) {
			throw new IllegalArgumentException("index must not be negative but was " + index);
		}

		Random random = randomFor(seed, index, type);
		Variant variant = switch (type) {
			case P1_RENAME_VARIABLES -> new Variant(type, renameVariables(original), RenderStyle.STANDARD);
			case P2_TYPOS -> new Variant(type, mapPrompts(original, text -> addTypos(text, random)),
					RenderStyle.STANDARD);
			case P3_POLITE_FILLER -> new Variant(type, mapPrompts(original, text -> makePolite(text, random)),
					RenderStyle.STANDARD);
			case P4_SWAP_NAME -> new Variant(type, swapName(original, random), RenderStyle.STANDARD);
			case P5_LAYOUT -> new Variant(type, original, RenderStyle.ALTERNATE);
			case S1_REMOVE_TEST_RUNS -> new Variant(type, removeTestRuns(original), RenderStyle.STANDARD);
			case S2_LAZY_PROMPTS -> new Variant(type, mapPrompts(original, text -> pick(Phrases.LAZY_PROMPTS, random)),
					RenderStyle.STANDARD);
		};

		boolean changed = !variant.text().equals(SessionRenderer.render(original));
		return changed ? Optional.of(variant) : Optional.empty();
	}

	/** Every applicable change of one kind, in the fixed order of {@link PerturbationType}. */
	public static List<Variant> applyAll(PerturbationKind kind, Session original, long seed, int index) {
		Objects.requireNonNull(kind, "kind");
		List<Variant> variants = new ArrayList<>();
		for (PerturbationType type : PerturbationType.ofKind(kind)) {
			apply(type, original, seed, index).ifPresent(variants::add);
		}
		return List.copyOf(variants);
	}

	// ---- the seven changes ---------------------------------------------

	/** P1: every variable name gets "Value" added. Adding a suffix can never make two different names equal. */
	private static Session renameVariables(Session session) {
		List<Event> events = new ArrayList<>();
		for (Event event : session.events()) {
			if (event instanceof Event.AiReply reply) {
				events.add(new Event.AiReply(reply.explanation(), renamed(reply.code())));
			} else {
				events.add(event);
			}
		}
		return session.withEvents(events);
	}

	private static CodeSnippet renamed(CodeSnippet code) {
		Map<String, String> names = new TreeMap<>();
		code.names().forEach((key, name) -> names.put(key, name + "Value"));
		return code.withNames(names);
	}

	/**
	 * P2: in each prompt, swap two neighbouring letters inside a few words. The first
	 * and last letter of a word never move, as in a real slip of the fingers. If a
	 * prompt has any word that can be swapped, at least one is.
	 */
	private static String addTypos(String text, Random random) {
		List<int[]> words = swappableWords(text);
		if (words.isEmpty()) {
			return text;
		}
		boolean[] chosen = new boolean[words.size()];
		boolean any = false;
		for (int i = 0; i < words.size(); i++) {
			if (random.nextInt(4) == 0) {
				chosen[i] = true;
				any = true;
			}
		}
		if (!any) {
			chosen[random.nextInt(words.size())] = true;
		}

		char[] letters = text.toCharArray();
		for (int i = 0; i < words.size(); i++) {
			if (chosen[i]) {
				int position = pick(swapPositions(letters, words.get(i)[0], words.get(i)[1]), random);
				char held = letters[position];
				letters[position] = letters[position + 1];
				letters[position + 1] = held;
			}
		}
		return new String(letters);
	}

	/** Words of four or more letters that have a pair of different neighbouring letters to swap. Each is {start, end}, end excluded. */
	private static List<int[]> swappableWords(String text) {
		List<int[]> words = new ArrayList<>();
		int i = 0;
		while (i < text.length()) {
			if (!isLetter(text.charAt(i))) {
				i++;
				continue;
			}
			int start = i;
			while (i < text.length() && isLetter(text.charAt(i))) {
				i++;
			}
			if (i - start >= 4 && !swapPositions(text.toCharArray(), start, i).isEmpty()) {
				words.add(new int[] { start, i });
			}
		}
		return words;
	}

	/** Positions p where letters p and p+1 differ, leaving the word's first and last letter alone. */
	private static List<Integer> swapPositions(char[] letters, int start, int end) {
		List<Integer> positions = new ArrayList<>();
		for (int p = start + 1; p <= end - 3; p++) {
			if (letters[p] != letters[p + 1]) {
				positions.add(p);
			}
		}
		return positions;
	}

	private static boolean isLetter(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
	}

	/** P3: a greeting before and thanks after, adding a full stop first if the prompt has no ending mark. */
	private static String makePolite(String text, Random random) {
		String prefix = pick(POLITE_PREFIXES, random);
		String suffix = pick(POLITE_SUFFIXES, random);
		char last = text.charAt(text.length() - 1);
		String body = (last == '.' || last == '!' || last == '?') ? text : text + ".";
		return prefix + body + " " + suffix;
	}

	/** P4: a different name from the fixed pool. */
	private static Session swapName(Session session, Random random) {
		List<String> others = CandidateNames.POOL.stream()
				.filter(name -> !name.equals(session.candidateName()))
				.toList();
		return session.withCandidateName(pick(others, random));
	}

	/** S1: the session without any test run. */
	private static Session removeTestRuns(Session session) {
		List<Event> kept = new ArrayList<>();
		for (Event event : session.events()) {
			if (!(event instanceof Event.TestRun)) {
				kept.add(event);
			}
		}
		return kept.isEmpty() ? session : session.withEvents(kept);
	}

	// ---- helpers ---------------------------------------------------------

	private static Session mapPrompts(Session session, UnaryOperator<String> change) {
		List<Event> events = new ArrayList<>();
		for (Event event : session.events()) {
			if (event instanceof Event.Prompt prompt) {
				events.add(new Event.Prompt(change.apply(prompt.text())));
			} else {
				events.add(event);
			}
		}
		return session.withEvents(events);
	}

	/**
	 * The random numbers for one change on one sample. The seed is spun forward to
	 * the sample, mixed with a fixed constant so the stream differs from the
	 * generator's, and spun forward again to the change's own stream number.
	 * {@link java.util.Random} is used because its algorithm is fixed by the Java
	 * specification, so the result is the same on every Java version.
	 */
	private static Random randomFor(long seed, int index, PerturbationType type) {
		Random root = new Random(seed);
		long sampleSeed = 0L;
		for (int step = 0; step <= index; step++) {
			sampleSeed = root.nextLong();
		}
		Random perSample = new Random(sampleSeed ^ SALT);
		long changeSeed = 0L;
		for (int step = 0; step < type.stream(); step++) {
			changeSeed = perSample.nextLong();
		}
		return new Random(changeSeed);
	}

	private static <T> T pick(List<T> options, Random random) {
		return options.get(random.nextInt(options.size()));
	}

}