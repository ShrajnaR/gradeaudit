package com.gradeaudit.sample;

import java.util.Objects;

import com.gradeaudit.common.Grade;

/**
 * One generated practice sample: its position in the audit, the behaviour it
 * was built from, and the session itself.
 */
public record GeneratedSample(int index, Template template, Session session) {

	public GeneratedSample {
		if (index < 0) {
			throw new IllegalArgumentException("index must not be negative but was " + index);
		}
		Objects.requireNonNull(template, "template");
		Objects.requireNonNull(session, "session");
	}

	/** The grade this sample is designed to deserve. */
	public Grade designedGrade() {
		return template.designedGrade();
	}

}