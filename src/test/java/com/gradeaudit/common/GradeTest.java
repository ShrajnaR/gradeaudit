package com.gradeaudit.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GradeTest {

	@Test
	void ranksRunFromBestToWorst() {
		assertEquals(0, Grade.A.rank());
		assertEquals(1, Grade.B.rank());
		assertEquals(2, Grade.C.rank());
	}

	@Test
	void worseMeansStrictlyHigherRank() {
		assertTrue(Grade.C.isWorseThan(Grade.B));
		assertTrue(Grade.B.isWorseThan(Grade.A));
		assertTrue(Grade.C.isWorseThan(Grade.A));
	}

	@Test
	void sameOrBetterIsNotWorse() {
		assertFalse(Grade.B.isWorseThan(Grade.B));
		assertFalse(Grade.A.isWorseThan(Grade.B));
		assertFalse(Grade.B.isWorseThan(Grade.C));
	}

}