package com.gradeaudit.perturb;

import java.util.Objects;

import com.gradeaudit.sample.RenderStyle;
import com.gradeaudit.sample.Session;
import com.gradeaudit.sample.SessionRenderer;

/**
 * One changed version of a session, ready to be sent to a grader.
 *
 * @param type    which change produced it
 * @param session the session after the change
 * @param style   the layout it is written in
 */
public record Variant(PerturbationType type, Session session, RenderStyle style) {

	public Variant {
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(session, "session");
		Objects.requireNonNull(style, "style");
	}

	/** The exact text a grader receives for this variant. */
	public String text() {
		return SessionRenderer.render(session, style);
	}

}