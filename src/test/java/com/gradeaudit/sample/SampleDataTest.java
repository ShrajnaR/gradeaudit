package com.gradeaudit.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Checks the fixed wording, names and tasks that the generator draws from. */
class SampleDataTest {

	@Test
	void everyTaskRendersWithEveryNameChoice() {
		for (Task task : Task.ALL) {
			Map<String, String> firstChoices = new HashMap<>();
			task.namePools().forEach((key, options) -> firstChoices.put(key, options.get(0)));

			assertNoPlaceholderLeft(task, firstChoices);
			for (Map.Entry<String, List<String>> pool : task.namePools().entrySet()) {
				assertTrue(task.codeTemplate().contains("__" + pool.getKey() + "__"),
						task.title() + " has a name pool for an unused placeholder: " + pool.getKey());
				for (String option : pool.getValue()) {
					Map<String, String> names = new HashMap<>(firstChoices);
					names.put(pool.getKey(), option);
					assertNoPlaceholderLeft(task, names);
				}
			}
		}
	}

	private static void assertNoPlaceholderLeft(Task task, Map<String, String> names) {
		String code = new CodeSnippet(task.codeTemplate(), names).render();

		assertFalse(code.contains("__"), task.title() + " still has a placeholder: " + code);
		assertNotEquals(task.codeTemplate(), code);
	}

	@Test
	void taskTitlesAndPromptsAreDistinct() {
		assertEquals(Task.ALL.size(), new HashSet<>(Task.ALL.stream().map(Task::title).toList()).size());
		assertEquals(Task.ALL.size(), new HashSet<>(Task.ALL.stream().map(Task::clearPrompt).toList()).size());
	}

	@Test
	void everyPhraseListIsFilledWithDistinctNonBlankText() {
		for (List<String> phrases : List.of(Phrases.FIRST_REPLIES, Phrases.FIXED_REPLIES, Phrases.SPECIFIC_FOLLOW_UPS,
				Phrases.MEDIUM_FOLLOW_UPS, Phrases.LAZY_PROMPTS)) {
			assertFalse(phrases.isEmpty());
			assertEquals(phrases.size(), new HashSet<>(phrases).size(), "duplicates in " + phrases);
			assertTrue(phrases.stream().noneMatch(String::isBlank));
		}
	}

	@Test
	void lazyPromptsReallyAreShort() {
		assertTrue(Phrases.LAZY_PROMPTS.stream().allMatch(prompt -> prompt.length() <= 20));
	}

	@Test
	void candidateNamesAreDistinctAndNotBlank() {
		assertEquals(8, CandidateNames.POOL.size());
		assertEquals(8, new HashSet<>(CandidateNames.POOL).size());
		assertTrue(CandidateNames.POOL.stream().noneMatch(String::isBlank));
	}

}