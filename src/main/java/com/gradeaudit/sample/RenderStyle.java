package com.gradeaudit.sample;

/**
 * How a session is written out as text. Both styles carry exactly the same
 * information. They differ only in punctuation and spacing, so grading the
 * same session in both styles tests whether a grader is distracted by layout.
 */
public enum RenderStyle {

	/** Square-bracket numbering and upper-case labels. */
	STANDARD,

	/** "Step" numbering, arrows, blank lines and fenced code. */
	ALTERNATE

}