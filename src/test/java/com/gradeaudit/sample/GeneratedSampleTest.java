package com.gradeaudit.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.gradeaudit.common.Grade;

class GeneratedSampleTest {

	private static final Session SESSION = new Session("Name", "Task", List.of(new Event.Prompt("hi")));

	@Test
	void designedGradeComesFromTheTemplate() {
		assertEquals(Grade.A, new GeneratedSample(0, Template.CAREFUL_VERIFIER, SESSION).designedGrade());
		assertEquals(Grade.C, new GeneratedSample(2, Template.BLIND_PASTER, SESSION).designedGrade());
	}

	@Test
	void invalidValuesAreRejected() {
		assertThrows(IllegalArgumentException.class, () -> new GeneratedSample(-1, Template.MIXED, SESSION));
		assertThrows(NullPointerException.class, () -> new GeneratedSample(0, null, SESSION));
		assertThrows(NullPointerException.class, () -> new GeneratedSample(0, Template.MIXED, null));
	}

}