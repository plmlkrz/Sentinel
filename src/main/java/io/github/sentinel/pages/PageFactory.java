package io.github.sentinel.pages;

import java.util.HashMap;

/**
 * The Page Factory is a factory method that simply takes a string containing the name of a
 * Page Object and returns the object to be worked on. It handles searching packages for page definitions.
 */
public class PageFactory {
	private static final ThreadLocal<HashMap<String, Page>> pages =
		ThreadLocal.withInitial(HashMap::new);

	private PageFactory() {
		//Exists only to defeat instantiation.
	}

	/**
	 * Returns the Page Object for the page name. This allows us to operate on pages
	 * without knowing they exist when we write step definitions.
	 *
	 * @param pageName String the name of the page object
	 * @return Page the page object
	 */
	protected static Page buildOrRetrievePage(String pageName) {
		pageName = pageName.replaceAll("\\s", "");
		Page page = pages.get().get(pageName);
		if (page != null) {
			return page;
		} else {
			page = new Page(pageName);
		}
		pages.get().put(pageName, page);
		return page;
	}

	/**
	 * Removes the per-thread page cache. Call from test teardown to prevent ThreadLocal leaks.
	 */
	public static void reset() {
		pages.remove();
	}
}
