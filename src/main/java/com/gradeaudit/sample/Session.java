package com.gradeaudit.sample;

import java.util.List;

/**
 * A record of one practice assessment: who took it, what task they were given,
 * and what happened, in order.
 *
 * <p>Immutable. The "with" methods return a changed copy, which is how
 * perturbations produce variants without touching the original.
 *
 * <p>A blank text is rejected with IllegalArgumentException. A missing (null)
 * list or event is rejected with NullPointerException.
 */
public record Session(String candidateName, String taskTitle, List<Event> events) {

	public Session {
		if (candidateName == null || candidateName.isBlank()) {
			throw new IllegalArgumentException("candidateName must not be blank");
		}
		if (taskTitle == null || taskTitle.isBlank()) {
			throw new IllegalArgumentException("taskTitle must not be blank");
		}
		events = List.copyOf(events);
		if (events.isEmpty()) {
			throw new IllegalArgumentException("events must not be empty");
		}
	}

	/** The same session for a different candidate. */
	public Session withCandidateName(String newName) {
		return new Session(newName, taskTitle, events);
	}

	/** The same session with a different list of events. */
	public Session withEvents(List<Event> newEvents) {
		return new Session(candidateName, taskTitle, newEvents);
	}

}