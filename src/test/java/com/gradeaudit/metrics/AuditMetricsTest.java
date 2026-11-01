package com.gradeaudit.metrics;

import static com.gradeaudit.metrics.TestGrades.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.gradeaudit.common.Grade;

class AuditMetricsTest {

	private static final double DELTA = 1e-9;

	// ---- originalGrade ------------------------------------------------

	@Test
	void originalGrade_isTheMiddleGradeOfThreeRepeats() {
		assertEquals(Grade.B, AuditMetrics.originalGrade(of("ABC")));
		assertEquals(Grade.B, AuditMetrics.originalGrade(of("CBA")));
	}

	@Test
	void originalGrade_ignoresOneOddRun() {
		assertEquals(Grade.A, AuditMetrics.originalGrade(of("AAC")));
	}

	@Test
	void originalGrade_withEvenCount_takesTheBetterMiddleGrade() {
		assertEquals(Grade.A, AuditMetrics.originalGrade(of("AC")));
		assertEquals(Grade.B, AuditMetrics.originalGrade(of("CABB")));
	}

	@Test
	void originalGrade_ofOneRepeat_isThatRepeat() {
		assertEquals(Grade.C, AuditMetrics.originalGrade(of("C")));
	}

	@Test
	void originalGrade_doesNotChangeTheInputList() {
		List<Grade> repeats = of("CBA");
		AuditMetrics.originalGrade(repeats);
		assertEquals(of("CBA"), repeats);
	}

	@Test
	void originalGrade_withNoRepeats_isRejected() {
		assertThrows(IllegalArgumentException.class, () -> AuditMetrics.originalGrade(List.of()));
		assertThrows(IllegalArgumentException.class, () -> AuditMetrics.originalGrade(null));
	}

	// ---- repeatability ------------------------------------------------

	@Test
	void repeatability_isMeanOfPairwiseLinearKappa() {
		var runs = List.of(of("AABBCCABCA"), of("AABBCCBBCA"), of("ABBBCCABCC"));
		assertEquals(0.6922799422799423, AuditMetrics.repeatability(runs), DELTA);
	}

	@Test
	void repeatability_ofIdenticalRuns_isOne() {
		var runs = List.of(of("AABBCC"), of("AABBCC"), of("AABBCC"));
		assertEquals(1.0, AuditMetrics.repeatability(runs), DELTA);
	}

	@Test
	void repeatability_ofAConstantGrader_isOne() {
		var runs = List.of(of("BBBB"), of("BBBB"));
		assertEquals(1.0, AuditMetrics.repeatability(runs), DELTA);
	}

	@Test
	void repeatability_needsAtLeastTwoRuns() {
		assertThrows(IllegalArgumentException.class, () -> AuditMetrics.repeatability(List.of(of("ABC"))));
		assertThrows(IllegalArgumentException.class, () -> AuditMetrics.repeatability(null));
	}

	// ---- flipRate -----------------------------------------------------

	@Test
	void flipRate_countsVariantsWithADifferentGrade() {
		assertEquals(0.5, AuditMetrics.flipRate(of("ABCA"), of("ACCB")), DELTA);
	}

	@Test
	void flipRate_ofUnchangedGrades_isZero() {
		assertEquals(0.0, AuditMetrics.flipRate(of("ABC"), of("ABC")), DELTA);
	}

	@Test
	void flipRate_countsImprovementsAsFlipsToo() {
		assertEquals(1.0, AuditMetrics.flipRate(of("CC"), of("AB")), DELTA);
	}

	@Test
	void flipRate_rejectsBadInput() {
		assertThrows(IllegalArgumentException.class, () -> AuditMetrics.flipRate(of("AB"), of("A")));
		assertThrows(IllegalArgumentException.class, () -> AuditMetrics.flipRate(List.of(), List.of()));
		assertThrows(IllegalArgumentException.class, () -> AuditMetrics.flipRate(null, of("A")));
	}

	// ---- dropRate -----------------------------------------------------

	@Test
	void dropRate_countsStrictlyWorseVariantsAmongAOrBOriginals() {
		// A->B drop, B->B no drop, C is skipped, A->C drop: 2 of 3
		assertEquals(2.0 / 3.0, AuditMetrics.dropRate(of("ABCA"), of("BBCC")), DELTA);
	}

	@Test
	void dropRate_doesNotCountImprovements() {
		assertEquals(0.0, AuditMetrics.dropRate(of("BB"), of("AA")), DELTA);
	}

	@Test
	void dropRate_ofAConstantGraderThatNeverReacts_isZero() {
		assertEquals(0.0, AuditMetrics.dropRate(of("BBBB"), of("BBBB")), DELTA);
	}

	@Test
	void dropRate_whenEveryOriginalIsC_isRejected() {
		assertThrows(IllegalArgumentException.class, () -> AuditMetrics.dropRate(of("CC"), of("CC")));
	}

	@Test
	void dropRate_rejectsBadInput() {
		assertThrows(IllegalArgumentException.class, () -> AuditMetrics.dropRate(of("AB"), of("A")));
		assertThrows(IllegalArgumentException.class, () -> AuditMetrics.dropRate(List.of(), List.of()));
	}

	// ---- agreement ----------------------------------------------------

	@Test
	void agreement_isLinearWeightedKappa() {
		assertEquals(0.6590909090909092, AuditMetrics.agreement(of("AAABBBCCCA"), of("AABBBCCCBA")), DELTA);
	}

}