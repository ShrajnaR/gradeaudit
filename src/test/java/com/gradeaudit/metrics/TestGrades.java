package com.gradeaudit.metrics;

import java.util.ArrayList;
import java.util.List;

import com.gradeaudit.common.Grade;

/** Test helper: turns a string such as "AABC" into a list of grades. */
final class TestGrades {

	private TestGrades() {
	}

	static List<Grade> of(String letters) {
		List<Grade> grades = new ArrayList<>();
		for (char letter : letters.toCharArray()) {
			grades.add(Grade.valueOf(String.valueOf(letter)));
		}
		return grades;
	}

}
