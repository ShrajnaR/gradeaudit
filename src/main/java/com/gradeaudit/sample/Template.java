package com.gradeaudit.sample;

import com.gradeaudit.common.Grade;

/**
 * The four ways a practice candidate can behave. Each one carries the grade
 * that behaviour is designed to deserve, which is the "right answer" the
 * audit's agreement check compares a grader against.
 */
public enum Template {

	/** Clear prompts, tests after every AI change, fixes failures. */
	CAREFUL_VERIFIER(Grade.A),

	/** Tests some of the AI code and skips others. */
	MIXED(Grade.B),

	/** Pastes large AI code with lazy prompts and never runs a test. */
	BLIND_PASTER(Grade.C),

	/** Runs the tests, sees them fail, and keeps pasting anyway. */
	IGNORES_FAILURES(Grade.C);

	private final Grade designedGrade;

	Template(Grade designedGrade) {
		this.designedGrade = designedGrade;
	}

	/** The grade this behaviour is designed to deserve. */
	public Grade designedGrade() {
		return designedGrade;
	}

	/**
	 * The template for the sample at this position. The four templates repeat in
	 * order, so any number of samples stays evenly balanced.
	 */
	public static Template forIndex(int index) {
		if (index < 0) {
			throw new IllegalArgumentException("index must not be negative but was " + index);
		}
		return values()[index % values().length];
	}

}