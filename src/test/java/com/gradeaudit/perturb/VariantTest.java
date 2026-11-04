package com.gradeaudit.perturb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.gradeaudit.sample.Event;
import com.gradeaudit.sample.RenderStyle;
import com.gradeaudit.sample.Session;
import com.gradeaudit.sample.SessionRenderer;

class VariantTest {

	private static final Session SESSION = new Session("Name", "Task", List.of(new Event.Prompt("hello")));

	@Test
	void textIsTheSessionRenderedInTheVariantsStyle() {
		var standard = new Variant(PerturbationType.P4_SWAP_NAME, SESSION, RenderStyle.STANDARD);
		var alternate = new Variant(PerturbationType.P5_LAYOUT, SESSION, RenderStyle.ALTERNATE);

		assertEquals(SessionRenderer.render(SESSION, RenderStyle.STANDARD), standard.text());
		assertEquals(SessionRenderer.render(SESSION, RenderStyle.ALTERNATE), alternate.text());
	}

	@Test
	void missingValuesAreRejected() {
		assertThrows(NullPointerException.class, () -> new Variant(null, SESSION, RenderStyle.STANDARD));
		assertThrows(NullPointerException.class,
				() -> new Variant(PerturbationType.P1_RENAME_VARIABLES, null, RenderStyle.STANDARD));
		assertThrows(NullPointerException.class,
				() -> new Variant(PerturbationType.P1_RENAME_VARIABLES, SESSION, null));
	}

}