package io.github.sentinel.apis;

import java.util.HashMap;

/**
 * An implementation of the factory design pattern based on the PageFactory.
 *
 * @author Sentinel Framework
 *
 */
public class APIFactory {
	private static final ThreadLocal<HashMap<String, API>> apis =
		ThreadLocal.withInitial(HashMap::new);

	private APIFactory() {
		// Exists only to defeat instantiation
	}

	/**
	 * Returns the requested API object by name if we have already created it,
	 * otherwise it creates the API. Note that this creation just instantiates
	 * the object as we late-bind things inside the object.
	 *
	 * @param apiName String the exact case-sensitive name of the yaml file containing the API information.
	 * @return API the API object that was created or retrieved
	 */
	public static API buildOrRetrieveAPI(String apiName) {
		apiName = apiName.replaceAll("\\s", "");
		API api = apis.get().get(apiName);
		if (api != null) {
			return api;
		} else {
			api = new API(apiName);
		}
		apis.get().put(apiName, api);
		return api;
	}

	/**
	 * Removes the per-thread API cache. Call from test teardown to prevent ThreadLocal leaks.
	 */
	public static void reset() {
		apis.remove();
	}
}
