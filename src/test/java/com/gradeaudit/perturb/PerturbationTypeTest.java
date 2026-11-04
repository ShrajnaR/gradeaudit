package com.gradeaudit.perturb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class PerturbationTypeTest {

	@Test
	void fiveHarmlessChangesInAFixedOrder() {
		assertEquals(List.of(PerturbationType.P1_RENAME_VARIABLES, PerturbationType.P2_TYPOS,
				PerturbationType.P3_POLITE_FILLER, PerturbationType.P4_SWAP_NAME, PerturbationType.P5_LAYOUT),
				PerturbationType.ofKind(PerturbationKind.INVARIANCE));
	}

	@Test
	void twoHarmfulChangesInAFixedOrder() {
		assertEquals(List.of(PerturbationType.S1_REMOVE_TEST_RUNS, PerturbationType.S2_LAZY_PROMPTS),
				PerturbationType.ofKind(PerturbationKind.SENSITIVITY));
	}

	@Test
	void codesAreTheShortNamesUsedInTheDesign() {
		assertEquals(List.of("P1", "P2", "P3", "P4", "P5", "S1", "S2"),
				Arrays.stream(PerturbationType.values()).map(PerturbationType::code).toList());
	}

	@Test
	void codesAndStreamNumbersAreUnique() {
		Set<String> codes = new HashSet<>();
		Set<Integer> streams = new HashSet<>();
		for (PerturbationType type : PerturbationType.values()) {
			codes.add(type.code());
			streams.add(type.stream());
		}
		assertEquals(PerturbationType.values().length, codes.size());
		assertEquals(PerturbationType.values().length, streams.size());
	}

	@Test
	void everyChangeHasTheKindItsNameSuggests() {
		for (PerturbationType type : PerturbationType.values()) {
			PerturbationKind expected = type.code().startsWith("P") ? PerturbationKind.INVARIANCE
					: PerturbationKind.SENSITIVITY;
			assertEquals(expected, type.kind());
		}
	}

}