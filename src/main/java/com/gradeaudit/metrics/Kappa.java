package com.gradeaudit.metrics;

import java.util.List;

import com.gradeaudit.common.Grade;

/**
 * Cohen's kappa: agreement between two raters, corrected for the agreement
 * that chance alone would produce.
 *
 * <pre>
 * kappa = 1 - (sum of w[i][j] * observed[i][j]) / (sum of w[i][j] * expected[i][j])
 * </pre>
 *
 * where observed and expected are proportions, and expected[i][j] is the share
 * of rater one's grade i times the share of rater two's grade j.
 *
 * <p>Two weightings are offered. Unweighted kappa treats every disagreement as
 * equally bad. Linear weighted kappa punishes A against C twice as much as A
 * against B, which suits ordered grades.
 *
 * <p>If both raters use one identical grade for every item, the expected
 * disagreement is zero and the ratio is undefined. That is defined here as 1.0
 * (perfect agreement), as written in the design document.
 *
 * <p>Plain Java on purpose: no Spring, so it is tested without a context.
 */
public final class Kappa {

	private Kappa() {
	}

	/** Unweighted kappa: any disagreement counts the same. */
	public static double cohen(List<Grade> raterOne, List<Grade> raterTwo) {
		return compute(raterOne, raterTwo, false);
	}

	/** Linear weighted kappa: the penalty grows with the distance between grades. */
	public static double linearWeighted(List<Grade> raterOne, List<Grade> raterTwo) {
		return compute(raterOne, raterTwo, true);
	}

	private static double compute(List<Grade> raterOne, List<Grade> raterTwo, boolean weighted) {
		validate(raterOne, raterTwo);

		int categories = Grade.values().length;
		double total = raterOne.size();

		double[][] observed = new double[categories][categories];
		double[] rowTotals = new double[categories];
		double[] columnTotals = new double[categories];

		for (int i = 0; i < raterOne.size(); i++) {
			int row = raterOne.get(i).rank();
			int column = raterTwo.get(i).rank();
			observed[row][column] += 1.0;
			rowTotals[row] += 1.0;
			columnTotals[column] += 1.0;
		}

		double observedDisagreement = 0.0;
		double expectedDisagreement = 0.0;
		for (int row = 0; row < categories; row++) {
			for (int column = 0; column < categories; column++) {
				double weight = weight(row, column, categories, weighted);
				observedDisagreement += weight * observed[row][column] / total;
				expectedDisagreement += weight * (rowTotals[row] / total) * (columnTotals[column] / total);
			}
		}

		if (expectedDisagreement == 0.0) {
			return 1.0;
		}
		return 1.0 - observedDisagreement / expectedDisagreement;
	}

	private static double weight(int row, int column, int categories, boolean weighted) {
		if (weighted) {
			return Math.abs(row - column) / (double) (categories - 1);
		}
		return row == column ? 0.0 : 1.0;
	}

	private static void validate(List<Grade> raterOne, List<Grade> raterTwo) {
		if (raterOne == null || raterTwo == null) {
			throw new IllegalArgumentException("Grade lists must not be null");
		}
		if (raterOne.size() != raterTwo.size()) {
			throw new IllegalArgumentException(
					"Grade lists must have the same size but were " + raterOne.size() + " and " + raterTwo.size());
		}
		if (raterOne.isEmpty()) {
			throw new IllegalArgumentException("Grade lists must not be empty");
		}
	}

}