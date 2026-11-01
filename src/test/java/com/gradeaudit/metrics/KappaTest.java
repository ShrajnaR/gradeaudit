package com.gradeaudit.metrics;

import static com.gradeaudit.metrics.TestGrades.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.gradeaudit.common.Grade;

/**
 * The expected numbers were computed independently with scikit-learn's
 * cohen_kappa_score (labels A, B, C; weights none or linear).
 */
class KappaTest {

	private static final double DELTA = 1e-9;

	@Test
	void publishedWorkedExample_rawAgreement070_chance050_gives040() {
		// 20 items: 7 AA, 3 AB, 3 BA, 7 BB. Observed 0.70, expected by chance 0.50.
		var one = of("AAAAAAAAAABBBBBBBBBB");
		var two = of("AAAAAAABBBAAABBBBBBB");

		assertEquals(0.4, Kappa.cohen(one, two), DELTA);
		assertEquals(0.4, Kappa.linearWeighted(one, two), DELTA);
	}

	@Test
	void threeGrades_unweighted() {
		assertEquals(0.5522388059701492, Kappa.cohen(of("AAABBBCCCA"), of("AABBBCCCBA")), DELTA);
	}

	@Test
	void threeGrades_linearWeighted() {
		assertEquals(0.6590909090909092, Kappa.linearWeighted(of("AAABBBCCCA"), of("AABBBCCCBA")), DELTA);
	}

	@Test
	void oppositeRatings_giveNegativeKappa() {
		assertEquals(-0.04477611940298498, Kappa.cohen(of("AAABBBCCCC"), of("CCCBBBAAAA")), DELTA);
		assertEquals(-0.5217391304347825, Kappa.linearWeighted(of("AAABBBCCCC"), of("CCCBBBAAAA")), DELTA);
	}

	@Test
	void identicalRatings_giveOne() {
		assertEquals(1.0, Kappa.cohen(of("ABCABC"), of("ABCABC")), DELTA);
		assertEquals(1.0, Kappa.linearWeighted(of("ABCABC"), of("ABCABC")), DELTA);
	}

	@Test
	void bothRatersUseOneIdenticalGrade_isDefinedAsOne() {
		assertEquals(1.0, Kappa.cohen(of("BBBB"), of("BBBB")), DELTA);
		assertEquals(1.0, Kappa.linearWeighted(of("BBBB"), of("BBBB")), DELTA);
	}

	@Test
	void eachRaterConstantButDifferent_givesZero() {
		assertEquals(0.0, Kappa.cohen(of("AAAA"), of("CCCC")), DELTA);
		assertEquals(0.0, Kappa.linearWeighted(of("AAAA"), of("CCCC")), DELTA);
	}

	@Test
	void sizeMismatch_isRejected() {
		assertThrows(IllegalArgumentException.class, () -> Kappa.cohen(of("AB"), of("ABC")));
	}

	@Test
	void emptyLists_areRejected() {
		List<Grade> empty = List.of();
		assertThrows(IllegalArgumentException.class, () -> Kappa.linearWeighted(empty, empty));
	}

	@Test
	void nullLists_areRejected() {
		assertThrows(IllegalArgumentException.class, () -> Kappa.cohen(null, of("A")));
		assertThrows(IllegalArgumentException.class, () -> Kappa.cohen(of("A"), null));
	}

}