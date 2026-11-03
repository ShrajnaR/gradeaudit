package com.gradeaudit.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.gradeaudit.common.Grade;

class TemplateTest {

	@Test
	void eachBehaviourHasItsDesignedGrade() {
		assertEquals(Grade.A, Template.CAREFUL_VERIFIER.designedGrade());
		assertEquals(Grade.B, Template.MIXED.designedGrade());
		assertEquals(Grade.C, Template.BLIND_PASTER.designedGrade());
		assertEquals(Grade.C, Template.IGNORES_FAILURES.designedGrade());
	}

	@Test
	void templatesRepeatInOrderByIndex() {
		assertEquals(Template.CAREFUL_VERIFIER, Template.forIndex(0));
		assertEquals(Template.MIXED, Template.forIndex(1));
		assertEquals(Template.BLIND_PASTER, Template.forIndex(2));
		assertEquals(Template.IGNORES_FAILURES, Template.forIndex(3));
		assertEquals(Template.CAREFUL_VERIFIER, Template.forIndex(4));
		assertEquals(Template.IGNORES_FAILURES, Template.forIndex(39));
	}

	@Test
	void negativeIndexIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> Template.forIndex(-1));
	}

}