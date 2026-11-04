package com.gradeaudit.perturb;

/** What a change is expected to do to a grade. */
public enum PerturbationKind {

	/** A harmless change: the grade should stay the same. */
	INVARIANCE,

	/** A harmful change: the grade should get worse. */
	SENSITIVITY

}