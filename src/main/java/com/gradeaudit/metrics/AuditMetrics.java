package com.gradeaudit.metrics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.gradeaudit.common.Grade;

/**
 * The numbers behind the four audit checks. Every method is a pure function of
 * grade lists, so the same input always gives the same answer.
 *
 * <p>Methods throw {@link IllegalArgumentException} when a value cannot be
 * computed, for example when there is no data. The caller decides how to report
 * that; a missing number is never silently turned into zero.
 */
public final class AuditMetrics {

	private AuditMetrics() {
	}

	/**
	 * The original grade of one sample: the lower median of its repeat grades.
	 * For an even count the better of the two middle grades is taken. Median is
	 * used because grades are ordered, and it ignores one odd run.
	 */
	public static Grade originalGrade(List<Grade> repeatGrades) {
		if (repeatGrades == null || repeatGrades.isEmpty()) {
			throw new IllegalArgumentException("At least one repeat grade is required");
		}
		List<Grade> sorted = new ArrayList<>(repeatGrades);
		sorted.sort(Comparator.comparingInt(Grade::rank));
		return sorted.get((sorted.size() - 1) / 2);
	}

	/**
	 * Check 1, repeatability: the mean linear weighted kappa over every pair of
	 * repeat runs. Each inner list is one run, holding one grade per sample, in
	 * the same sample order in every run.
	 */
	public static double repeatability(List<List<Grade>> runs) {
		if (runs == null || runs.size() < 2) {
			throw new IllegalArgumentException("Repeatability needs at least two runs");
		}
		double sum = 0.0;
		int pairs = 0;
		for (int first = 0; first < runs.size(); first++) {
			for (int second = first + 1; second < runs.size(); second++) {
				sum += Kappa.linearWeighted(runs.get(first), runs.get(second));
				pairs++;
			}
		}
		return sum / pairs;
	}

	/**
	 * Check 2, invariance: the share of variants whose grade differs from the
	 * original grade. The two lists are aligned, so position i holds the
	 * original and the variant of the same sample.
	 */
	public static double flipRate(List<Grade> originals, List<Grade> variants) {
		requireAlignedAndNotEmpty(originals, variants);
		int flips = 0;
		for (int i = 0; i < originals.size(); i++) {
			if (originals.get(i) != variants.get(i)) {
				flips++;
			}
		}
		return (double) flips / originals.size();
	}

	/**
	 * Check 3, sensitivity: of the pairs whose original grade is A or B, the
	 * share where the variant is graded strictly worse. A sample already graded
	 * C cannot get worse, so it is left out of the count.
	 */
	public static double dropRate(List<Grade> originals, List<Grade> variants) {
		requireAlignedAndNotEmpty(originals, variants);
		int applicable = 0;
		int drops = 0;
		for (int i = 0; i < originals.size(); i++) {
			if (originals.get(i) == Grade.C) {
				continue;
			}
			applicable++;
			if (variants.get(i).isWorseThan(originals.get(i))) {
				drops++;
			}
		}
		if (applicable == 0) {
			throw new IllegalArgumentException("No applicable samples: every original grade is already C");
		}
		return (double) drops / applicable;
	}

	/**
	 * Check 4, agreement: linear weighted kappa between the grader's original
	 * grades and the grades the sessions were designed to deserve.
	 */
	public static double agreement(List<Grade> graderGrades, List<Grade> designedGrades) {
		return Kappa.linearWeighted(graderGrades, designedGrades);
	}

	private static void requireAlignedAndNotEmpty(List<Grade> originals, List<Grade> variants) {
		if (originals == null || variants == null) {
			throw new IllegalArgumentException("Grade lists must not be null");
		}
		if (originals.size() != variants.size()) {
			throw new IllegalArgumentException("Grade lists must have the same size but were " + originals.size()
					+ " and " + variants.size());
		}
		if (originals.isEmpty()) {
			throw new IllegalArgumentException("Grade lists must not be empty");
		}
	}

}