package io.github.sentinel.system;

public class TestManager {
	private static final ThreadLocal<YAMLObject> activeTestObject = new ThreadLocal<>();

	public static void setActiveTestObject(YAMLObject yamlObject) {
		activeTestObject.set(yamlObject);
	}
	public static YAMLObject getActiveTestObject() {
		return activeTestObject.get();
	}

	public static void reset() {
		activeTestObject.remove();
	}

	private TestManager() {
		// Exists only to defeat instantiation.
	}
}
