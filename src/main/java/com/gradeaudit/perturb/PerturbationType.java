package com.gradeaudit.perturb;

import java.util.Arrays;
import java.util.List;

/**
 * The seven changes the audit applies to a session.
 *
 * <p>The short code (P1, S2, ...) is what gets stored with each grader call. The
 * stream number picks the random numbers a change uses. It is written out here
 * on purpose, so reordering the constants can never change the output.
 */
public enum PerturbationType {

	/** Rename the variables in the AI's code. */
	P1_RENAME_VARIABLES("P1", 1, PerturbationKind.INVARIANCE),

	/** Swap two letters in a few words of the candidate's prompts. */
	P2_TYPOS("P2", 2, PerturbationKind.INVARIANCE),

	/** Wrap each prompt in a polite greeting and thanks. */
	P3_POLITE_FILLER("P3", 3, PerturbationKind.INVARIANCE),

	/** Give the session a different candidate name. */
	P4_SWAP_NAME("P4", 4, PerturbationKind.INVARIANCE),

	/** Write the same session in the alternate layout. */
	P5_LAYOUT("P5", 5, PerturbationKind.INVARIANCE),

	/** Remove every test run. */
	S1_REMOVE_TEST_RUNS("S1", 6, PerturbationKind.SENSITIVITY),

	/** Replace every prompt with a lazy one-liner. */
	S2_LAZY_PROMPTS("S2", 7, PerturbationKind.SENSITIVITY);

	private final String code;
	private final int stream;
	private final PerturbationKind kind;

	PerturbationType(String code, int stream, PerturbationKind kind) {
		this.code = code;
		this.stream = stream;
		this.kind = kind;
	}

	/** The short code stored with each grader call. */
	public String code() {
		return code;
	}

	public PerturbationKind kind() {
		return kind;
	}

	int stream() {
		return stream;
	}

	/** The changes of one kind, in a fixed order. */
	public static List<PerturbationType> ofKind(PerturbationKind kind) {
		return Arrays.stream(values()).filter(type -> type.kind == kind).toList();
	}

}