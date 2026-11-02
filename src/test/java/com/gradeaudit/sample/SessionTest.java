package com.gradeaudit.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class SessionTest {

	private static final List<Event> EVENTS = List.of(new Event.Prompt("hi"), new Event.TestRun(1, 2));

	@Test
	void validSessionKeepsItsValues() {
		var session = new Session("Priya Nair", "Sum of even numbers", EVENTS);

		assertEquals("Priya Nair", session.candidateName());
		assertEquals("Sum of even numbers", session.taskTitle());
		assertEquals(EVENTS, session.events());
	}

	@Test
	void blankNameOrTitleIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> new Session(" ", "Task", EVENTS));
		assertThrows(IllegalArgumentException.class, () -> new Session(null, "Task", EVENTS));
		assertThrows(IllegalArgumentException.class, () -> new Session("Name", "", EVENTS));
		assertThrows(IllegalArgumentException.class, () -> new Session("Name", null, EVENTS));
	}

	@Test
	void emptyEventListIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> new Session("Name", "Task", List.of()));
	}

	@Test
	void missingEventsAreRejected() {
		assertThrows(NullPointerException.class, () -> new Session("Name", "Task", null));
		assertThrows(NullPointerException.class,
				() -> new Session("Name", "Task", Arrays.asList(new Event.Prompt("hi"), null)));
	}

	@Test
	void eventListCannotBeChangedFromOutside() {
		var session = new Session("Name", "Task", EVENTS);

		assertThrows(UnsupportedOperationException.class, () -> session.events().add(new Event.ManualEdit(1)));
	}

	@Test
	void laterChangesToTheCallersListDoNotAffectTheSession() {
		List<Event> events = new ArrayList<>(EVENTS);
		var session = new Session("Name", "Task", events);

		events.add(new Event.ManualEdit(1));

		assertEquals(2, session.events().size());
	}

	@Test
	void withCandidateNameChangesOnlyTheName() {
		var original = new Session("Priya Nair", "Task", EVENTS);
		var changed = original.withCandidateName("Wei Zhang");

		assertEquals("Wei Zhang", changed.candidateName());
		assertEquals("Task", changed.taskTitle());
		assertEquals(EVENTS, changed.events());
		assertEquals("Priya Nair", original.candidateName());
		assertNotSame(original, changed);
	}

	@Test
	void withEventsChangesOnlyTheEvents() {
		var original = new Session("Name", "Task", EVENTS);
		var changed = original.withEvents(List.of(new Event.ManualEdit(2)));

		assertEquals(List.of(new Event.ManualEdit(2)), changed.events());
		assertEquals("Name", changed.candidateName());
		assertEquals(EVENTS, original.events());
	}

	@Test
	void sessionsWithEqualValuesAreEqual() {
		assertEquals(new Session("Name", "Task", EVENTS), new Session("Name", "Task", EVENTS));
	}

}