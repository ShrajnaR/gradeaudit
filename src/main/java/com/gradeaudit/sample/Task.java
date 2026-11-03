package com.gradeaudit.sample;

import java.util.List;
import java.util.Map;

/**
 * A small programming task that a practice candidate is given, with the code the
 * AI assistant "writes" for it.
 *
 * <p>The code template uses placeholders such as {@code __total__}. For each
 * placeholder there is a pool of equally good names to choose from.
 */
record Task(String title, String clearPrompt, String codeTemplate, Map<String, List<String>> namePools) {

	static final List<Task> ALL = List.of(

			new Task("Sum of even numbers",
					"Write a Java method that takes a list of integers and returns the sum of the even numbers. "
							+ "It must return 0 for an empty list and must not modify the input.",
					"""
							int __total__ = 0;
							for (int __item__ : __items__) {
							    if (__item__ % 2 == 0) {
							        __total__ += __item__;
							    }
							}
							return __total__;""",
					Map.of(
							"total", List.of("total", "sum", "acc"),
							"item", List.of("item", "value", "number"),
							"items", List.of("items", "values", "numbers"))),

			new Task("Count the non-blank words",
					"Write a Java method that counts the non-blank strings in a list of words. "
							+ "Null entries must be ignored, and an empty list must give 0.",
					"""
							int __count__ = 0;
							for (String __word__ : __words__) {
							    if (__word__ != null && !__word__.isBlank()) {
							        __count__++;
							    }
							}
							return __count__;""",
					Map.of(
							"count", List.of("count", "found", "tally"),
							"word", List.of("word", "entry", "token"),
							"words", List.of("words", "entries", "tokens"))),

			new Task("Find the largest value",
					"Write a Java method that returns the largest integer in a non-empty list "
							+ "and throws IllegalArgumentException for an empty list.",
					"""
							if (__items__.isEmpty()) {
							    throw new IllegalArgumentException("list is empty");
							}
							int __best__ = __items__.get(0);
							for (int __item__ : __items__) {
							    if (__item__ > __best__) {
							        __best__ = __item__;
							    }
							}
							return __best__;""",
					Map.of(
							"items", List.of("items", "values", "numbers"),
							"best", List.of("best", "max", "largest"),
							"item", List.of("item", "value", "number"))));

}