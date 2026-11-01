package com.gradeaudit.common;

/**
 * The grade a grader gives to one session.
 *
 * <p>Grades are ordinal: A is the best and C is the weakest. The rank is stored
 * explicitly, so reordering the constants can never change the maths.
 */
public enum Grade {

	A(0),
	B(1),
	C(2);

	private final int rank;

	Grade(int rank) {
		this.rank = rank;
	}

	/** 0 for the best grade, increasing as the grade gets worse. */
	public int rank() {
		return rank;
	}

	/** True if this grade is strictly worse than the other one. */
	public boolean isWorseThan(Grade other) {
		return this.rank > other.rank;
	}

}