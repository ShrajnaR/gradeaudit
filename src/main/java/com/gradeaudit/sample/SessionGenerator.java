package com.gradeaudit.sample;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Builds practice sessions from a seed. The same seed and index always give the
 * same session, on every computer and every run.
 *
 * <p>How that is guaranteed:
 * <ul>
 * <li>{@link java.util.Random} is used on purpose. Its algorithm is written into
 * the Java specification, so it gives identical numbers on every Java version.
 * Only the oldest methods ({@code nextInt(bound)} and {@code nextLong()}) are
 * used, because newer convenience methods are not covered by that promise.</li>
 * <li>Every sample gets its own generator, derived from the seed and its index,
 * so sample 7 is the same whether 10 or 1000 samples are asked for.</li>
 * <li>Name choices are made in sorted key order. Iterating a plain Map in its own
 * order would change from one Java run to the next.</li>
 * </ul>
 *
 * <p>Each candidate behaviour is a short fixed script of events with randomly
 * chosen details (task, names, wording, line counts, test counts).
 */
public final class SessionGenerator {

	private static final int TESTS_MIN = 5;
	private static final int TESTS_MAX = 8;

	private SessionGenerator() {
	}

	/** The first {@code count} samples for this seed. */
	public static List<GeneratedSample> generateAll(long seed, int count) {
		if (count < 1) {
			throw new IllegalArgumentException("count must be at least 1 but was " + count);
		}
		List<GeneratedSample> samples = new ArrayList<>(count);
		for (int index = 0; index < count; index++) {
			samples.add(generate(seed, index));
		}
		return List.copyOf(samples);
	}

	/** The sample at this position for this seed. */
	public static GeneratedSample generate(long seed, int index) {
		Template template = Template.forIndex(index);
		Random random = randomFor(seed, index);

		Task task = pick(Task.ALL, random);
		String candidate = pick(CandidateNames.POOL, random);
		CodeSnippet code = new CodeSnippet(task.codeTemplate(), chooseNames(task, random));

		List<Event> events = switch (template) {
			case CAREFUL_VERIFIER -> carefulVerifier(task, code, random);
			case MIXED -> mixed(task, code, random);
			case BLIND_PASTER -> blindPaster(code, random);
			case IGNORES_FAILURES -> ignoresFailures(task, code, random);
		};

		return new GeneratedSample(index, template, new Session(candidate, task.title(), events));
	}

	// ---- the four behaviours ------------------------------------------

	/** Every AI change is tested straight away, failures are described exactly, and it ends green. */
	private static List<Event> carefulVerifier(Task task, CodeSnippet code, Random random) {
		int total = between(random, TESTS_MIN, TESTS_MAX);
		List<Event> events = new ArrayList<>();
		events.add(new Event.Prompt(task.clearPrompt()));
		events.add(new Event.AiReply(pick(Phrases.FIRST_REPLIES, random), code));
		events.add(new Event.AiCodeApplied(firstApplyLines(code, random)));
		events.add(new Event.TestRun(someFailing(total, random), total));
		events.add(new Event.Prompt(pick(Phrases.SPECIFIC_FOLLOW_UPS, random)));
		events.add(new Event.AiReply(pick(Phrases.FIXED_REPLIES, random), code));
		events.add(new Event.AiCodeApplied(between(random, 3, 10)));
		events.add(new Event.TestRun(total, total));
		events.add(new Event.ManualEdit(between(random, 3, 8)));
		events.add(new Event.TestRun(total, total));
		return events;
	}

	/** The first AI change is tested; the second is not tested until after a manual edit. */
	private static List<Event> mixed(Task task, CodeSnippet code, Random random) {
		int total = between(random, TESTS_MIN, TESTS_MAX);
		List<Event> events = new ArrayList<>();
		events.add(new Event.Prompt(task.clearPrompt()));
		events.add(new Event.AiReply(pick(Phrases.FIRST_REPLIES, random), code));
		events.add(new Event.AiCodeApplied(between(random, 8, 20)));
		events.add(new Event.TestRun(someFailing(total, random), total));
		events.add(new Event.Prompt(pick(Phrases.MEDIUM_FOLLOW_UPS, random)));
		events.add(new Event.AiReply(pick(Phrases.FIXED_REPLIES, random), code));
		events.add(new Event.AiCodeApplied(between(random, 8, 25)));
		events.add(new Event.ManualEdit(between(random, 2, 6)));
		events.add(new Event.TestRun(total, total));
		return events;
	}

	/** Lazy prompts, large pastes, and no test run at all. */
	private static List<Event> blindPaster(CodeSnippet code, Random random) {
		List<Event> events = new ArrayList<>();
		for (int round = 0; round < 3; round++) {
			events.add(new Event.Prompt(pick(Phrases.LAZY_PROMPTS, random)));
			List<String> replies = round == 0 ? Phrases.FIRST_REPLIES : Phrases.FIXED_REPLIES;
			events.add(new Event.AiReply(pick(replies, random), code));
			events.add(new Event.AiCodeApplied(between(random, 35, 80)));
		}
		return events;
	}

	/** Tests run and fail every time, yet more large code is pasted without improving. */
	private static List<Event> ignoresFailures(Task task, CodeSnippet code, Random random) {
		int total = between(random, TESTS_MIN, TESTS_MAX);
		List<Event> events = new ArrayList<>();
		for (int round = 0; round < 3; round++) {
			if (round == 0) {
				events.add(new Event.Prompt(task.clearPrompt()));
				events.add(new Event.AiReply(pick(Phrases.FIRST_REPLIES, random), code));
			} else {
				events.add(new Event.Prompt(pick(Phrases.LAZY_PROMPTS, random)));
				events.add(new Event.AiReply(pick(Phrases.FIXED_REPLIES, random), code));
			}
			events.add(new Event.AiCodeApplied(between(random, 30, 60)));
			events.add(new Event.TestRun(mostlyFailing(total, random), total));
		}
		return events;
	}

	// ---- helpers --------------------------------------------------------

	/**
	 * A generator for this sample. The seed is spun forward index + 1 times and the
	 * last value becomes the sample's own seed, so each sample is independent of
	 * how many others are generated.
	 */
	private static Random randomFor(long seed, int index) {
		Random root = new Random(seed);
		long sampleSeed = 0L;
		for (int step = 0; step <= index; step++) {
			sampleSeed = root.nextLong();
		}
		return new Random(sampleSeed);
	}

	/** One name for each placeholder, chosen in sorted key order so the result never depends on map order. */
	private static Map<String, String> chooseNames(Task task, Random random) {
		Map<String, String> chosen = new TreeMap<>();
		for (Map.Entry<String, List<String>> pool : new TreeMap<>(task.namePools()).entrySet()) {
			chosen.put(pool.getKey(), pick(pool.getValue(), random));
		}
		return chosen;
	}

	private static int firstApplyLines(CodeSnippet code, Random random) {
		int codeLines = (int) code.render().lines().count();
		return codeLines + between(random, 0, 3);
	}

	/** A test count where two or three tests fail. */
	private static int someFailing(int total, Random random) {
		return between(random, Math.max(0, total - 3), total - 2);
	}

	/** A test count where at least two tests fail, often many more. */
	private static int mostlyFailing(int total, Random random) {
		return between(random, 0, total / 2);
	}

	private static <T> T pick(List<T> options, Random random) {
		return options.get(random.nextInt(options.size()));
	}

	/** A whole number from low to high, both included. */
	private static int between(Random random, int low, int high) {
		return low + random.nextInt(high - low + 1);
	}

}