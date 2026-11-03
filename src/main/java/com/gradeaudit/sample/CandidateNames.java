package com.gradeaudit.sample;

import java.util.List;

/**
 * Fictional candidate names, used to fill the candidate field and later to swap
 * one name for another.
 *
 * <p>A name is only a weak hint about a person's background, so swapping names
 * is a rough probe for unfair sensitivity and not a bias audit. The list is
 * fixed and ordered, so the same seed always picks the same name.
 */
public final class CandidateNames {

	public static final List<String> POOL = List.of(
			"Priya Nair",
			"Wei Zhang",
			"Aisha Khan",
			"Carlos Mendez",
			"Emily Carter",
			"James Walker",
			"Olga Petrova",
			"Kwame Mensah");

	private CandidateNames() {
	}

}