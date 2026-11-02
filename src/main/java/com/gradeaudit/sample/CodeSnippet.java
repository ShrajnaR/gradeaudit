package com.gradeaudit.sample;

import java.util.Map;
import java.util.Objects;

/**
 * A piece of code stored as a template plus the names used in it.
 *
 * <p>Placeholders look like {@code __total__} in the template, and the map says
 * which real name each one stands for. Keeping the names separate means a
 * perturbation can rename variables by changing the map. The code text itself
 * is never edited with pattern matching.
 *
 * <p>Names must be plain identifiers (letters and digits, starting with a
 * letter). That rules out underscores, so a name can never look like a
 * placeholder and the order of replacement cannot matter.
 */
public record CodeSnippet(String template, Map<String, String> names) {

	private static final String MARK = "__";

	public CodeSnippet {
		if (template == null || template.isBlank()) {
			throw new IllegalArgumentException("template must not be blank");
		}
		Objects.requireNonNull(names, "names");
		names = Map.copyOf(names);
		for (Map.Entry<String, String> entry : names.entrySet()) {
			requireIdentifier(entry.getKey(), "name key");
			requireIdentifier(entry.getValue(), "name for " + entry.getKey());
		}
		if (substitute(template, names).contains(MARK)) {
			throw new IllegalArgumentException("template has a placeholder without a name");
		}
	}

	/** The code with every placeholder replaced by its name. */
	public String render() {
		return substitute(template, names);
	}

	/** The same code with different names. */
	public CodeSnippet withNames(Map<String, String> newNames) {
		return new CodeSnippet(template, newNames);
	}

	private static String substitute(String template, Map<String, String> names) {
		String result = template;
		for (Map.Entry<String, String> entry : names.entrySet()) {
			result = result.replace(MARK + entry.getKey() + MARK, entry.getValue());
		}
		return result;
	}

	private static void requireIdentifier(String value, String what) {
		if (value == null || !value.matches("[A-Za-z][A-Za-z0-9]*")) {
			throw new IllegalArgumentException(what + " must be letters and digits, starting with a letter: " + value);
		}
	}

}