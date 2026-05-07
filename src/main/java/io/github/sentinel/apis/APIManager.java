package io.github.sentinel.apis;

import org.openqa.selenium.NotFoundException;

import io.github.sentinel.configurations.Configuration;
import io.github.sentinel.enums.RequestType;
import io.github.sentinel.system.TestManager;

import java.io.InputStream;

/**
 * Tracks which API is currently being used and requests the APIFactory create it if it does not exist.
 * @author Sentinel Framework
 *
 */
public class APIManager {
	private static final ThreadLocal<API> api = new ThreadLocal<>();
	private static final ThreadLocal<Response> response = new ThreadLocal<>();
	private static final ThreadLocal<String> lastCurlCommand = ThreadLocal.withInitial(() -> "");

	private APIManager() {
		// Exists only to defeat instantiation.
	}

	/**
	 * Stores an API using the passed API name to instantiate it.
	 * @param apiName String the name of the sentinel API object to create and store
	 */
	public static void setAPI(String apiName) {
		if (apiName == null || apiName.isBlank()) {
			throw new NotFoundException("API name cannot be null or blank.");
		}
		API builtApi = APIFactory.buildOrRetrieveAPI(apiName);
		api.set(builtApi);
		TestManager.setActiveTestObject(builtApi);
	}

	/**
	 * Returns the currently active API
	 * @return API Currently selected API by the tester.
	 */
	public static API getAPI() {
		if (api.get() == null)
			throw new NotFoundException("API not set yet. It must be created before it can be used.");
		return api.get();
	}

	/**
	 * Sets the body of a new request for the current API
	 * @param body String the body in the form of json
	 */
	public static void setBody(String body) {
		getAPI().getRequest().setBody(body);
	}

	/**
	 * Sets a GraphQL query as the request body, wrapping it in {"query":"..."} and
	 * adding a Content-Type: application/json header.
	 * @param query String the GraphQL query or mutation string
	 */
	public static void setGraphQLBody(String query) {
		getAPI().getRequest().setGraphQLBody(query);
	}

	/**
	 * Sets the body to a multipart/form-data body, using the given name, boundary, input stream, and filename.
	 * @param nameOfInput String name of the multipart segment.
	 * @param boundary String the multipart boundary.
	 * @param inputStream InputStream input stream of the file to upload.
	 * @param filename String name of the file being uploaded.
	 */
	public static void setMultipartFormDataBody(String nameOfInput, String boundary, InputStream inputStream, String filename) {
		getAPI().getRequest().setMultipartFormDataBody(nameOfInput, boundary, inputStream, filename);
	}

	/**
	 * Set a parameter and its value for a request.
	 * @param parameter String the parameter being passed
	 * @param value String the value to be passed
	 */
	public static void addParameter(String parameter, String value) {
		getAPI().getRequest().addParameter(parameter, value);
	}

	/**
	 * Set a header and its value for a request.
	 * @param name String the name being passed
	 * @param value String the value to be passed
	 */
	public static void addHeader(String name, String value) {
		getAPI().getRequest().addHeader(name, value);
	}

	/**
	 * Sets Basic authentication credentials for the current request.
	 * @param username String the username
	 * @param password String the password
	 */
	public static void setBasicAuth(String username, String password) {
		getAPI().getRequest().setBasicAuth(username, password);
	}

	/**
	 * Sets Bearer token authentication for the current request.
	 * @param token String the bearer token
	 */
	public static void setBearerToken(String token) {
		getAPI().getRequest().setBearerToken(token);
	}

	/**
	 * Sets an API key header for the current request.
	 * @param key String the header name
	 * @param value String the header value
	 */
	public static void setApiKeyHeader(String key, String value) {
		getAPI().getRequest().setApiKeyHeader(key, value);
	}

	/**
	 * Sets the retry policy for the next request.
	 * @param maxRetries int the maximum number of retries
	 * @param delayMs long the delay between retries in milliseconds
	 */
	public static void setRetry(int maxRetries, long delayMs) {
		getAPI().getRequest().setRetry(maxRetries, delayMs);
	}

	/**
	 * Configures the next request to trust all SSL certificates.
	 */
	public static void setTrustAllSsl() {
		getAPI().getRequest().setTrustAllSsl();
	}

	/**
	 * Adds a cookie to be sent with the next request.
	 * @param name String the cookie name
	 * @param value String the cookie value
	 */
	public static void addCookie(String name, String value) {
		getAPI().getRequest().addCookie(name, value);
	}

	/**
	 * Send a request of the given type. The response will be stored in a Response object
	 * that the APIManager can retrieve.
	 * @param type io.github.sentinel.enums.RequestType the type of request to send
	 * @param endpoint the endpoint to send the request
	 */
	public static void sendRequest(RequestType type, String endpoint) {
		getAPI().getRequest().createAndSendRequest(type, endpoint);
	}

	/**
	 * Extracts a value from the last response using a JSONPath expression and stores it in
	 * Configuration under the given variable name for use in subsequent steps.
	 * @param variableName String the configuration key to store the value under
	 * @param jsonPath String the JSONPath expression (e.g., "$.id")
	 */
	public static void extractFromResponse(String variableName, String jsonPath) {
		String value = getResponse().extract(jsonPath);
		Configuration.update(variableName, value != null ? value : "");
	}

	/**
	 * Returns the value of the named response header from the last response.
	 * @param name String the header name
	 * @return String the header value, or null
	 */
	public static String getResponseHeader(String name) {
		return getResponse().getHeader(name);
	}

	/**
	 * Returns the value of the named cookie from the last response's Set-Cookie headers.
	 * @param name String the cookie name
	 * @return String the cookie value, or null
	 */
	public static String getResponseCookie(String name) {
		return getResponse().getCookie(name);
	}

	/**
	 * Returns the most recent response.
	 * @return Response the response
	 */
	public static Response getResponse() {
		return response.get();
	}

	/**
	 * Sets the most recent response.
	 * @param resp Response the response
	 */
	public static void setResponse(Response resp) {
		response.set(resp);
	}

	/**
	 * Returns the curl command equivalent for the last request that was sent.
	 * @return String the curl command string
	 */
	public static String getLastCurlCommand() {
		return lastCurlCommand.get();
	}

	/**
	 * Stores the curl command equivalent for the last request.
	 * @param curlCommand String the curl command string
	 */
	public static void setLastCurlCommand(String curlCommand) {
		lastCurlCommand.set(curlCommand);
	}

	/**
	 * Clears all per-thread API state. Call from test teardown to prevent ThreadLocal leaks.
	 */
	public static void reset() {
		api.remove();
		response.remove();
		lastCurlCommand.remove();
	}
}
