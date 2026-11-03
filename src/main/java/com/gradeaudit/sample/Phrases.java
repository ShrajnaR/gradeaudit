package com.gradeaudit.sample;

import java.util.List;

/**
 * The wording the generator draws from. Kept apart from the generator so the
 * text can be read and changed without touching any logic.
 */
public final class Phrases {

	/** What the AI says the first time it answers. */
	public static final List<String> FIRST_REPLIES = List.of(
			"Here is a first version of the method.",
			"This is a first attempt that follows your description.",
			"Here is an initial implementation.");

	/** What the AI says when it answers again after a complaint. */
	public static final List<String> FIXED_REPLIES = List.of(
			"Here is an updated version that addresses the failing cases.",
			"I changed the method to handle the cases that were failing.",
			"This revised version fixes the problems you described.");

	/** A careful candidate's follow-up: says exactly what fails and what is wanted. */
	public static final List<String> SPECIFIC_FOLLOW_UPS = List.of(
			"Two tests fail: the empty input case and a case with negative numbers. "
					+ "Please fix the method for both and explain what was wrong.",
			"The failing tests are the empty input case and an input with duplicate values. "
					+ "Please correct the method for both and tell me what caused the failures.",
			"Tests for empty input and for a single element are failing. Please fix both and explain the cause.");

	/** A middling follow-up: says something is wrong, but not what. */
	public static final List<String> MEDIUM_FOLLOW_UPS = List.of(
			"Some tests are still failing, can you look at it again?",
			"Not all tests pass yet. Please check the method again.",
			"It still fails on a few cases, can you improve it?");

	/** A lazy prompt: no detail at all. */
	public static final List<String> LAZY_PROMPTS = List.of(
			"fix it",
			"write the code",
			"do it",
			"it still fails",
			"again",
			"make it work",
			"more");

	private Phrases() {
	}

}