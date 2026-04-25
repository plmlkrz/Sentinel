package io.github.sentinel.apis;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import io.github.sentinel.strings.SentinelStringUtils;

/**
 * Wrapper for an http response for testing the response.
 * @author Sentinel Framework
 *
 */
public class Response {

	private static final Logger log = LogManager.getLogger(Response.class.getName());

	protected HttpResponse httpResponse;
	protected String jsonResponse;
	protected Duration responseTime;

	/**
	 * @param httpResponse HttpResponse the API call response used to create this object
	 * @throws IOException if the parsing fails
	 */
	public Response(HttpResponse httpResponse) throws IOException {
		this.httpResponse = httpResponse;
		// Entity is null for responses with no body (e.g. 204 No Content)
		HttpEntity entity = this.httpResponse.getEntity();
		this.jsonResponse = (entity != null)
				? SentinelStringUtils.inputStreamToString(entity.getContent())
				: "";
	}

	/**
	 * Returns the http response as a String
	 * @return String the http response
	 */
	public String getResponse() {
		return jsonResponse;
	}

	/**
	 * Returns the response code from the response
	 * @return int the status of the response
	 */
	public int getResponseCode() {
		return httpResponse.getStatusLine().getStatusCode();
	}

	/**
	 * Sets the amount of time that the response took to get.
	 * @param duration Duration the response time
	 */
	public void setResponseTime(Duration duration) {
		responseTime = duration;
	}

	/**
	 * Returns the amount of time the response took to get.
	 * @return Duration the response time
	 */
	public Duration getResponseTime() {
		return responseTime;
	}

	/**
	 * Extracts a value from the JSON response using a JSONPath expression.
	 * @param jsonPath String the JSONPath expression (e.g., "$.name")
	 * @return String the extracted value as a String, or null if not found
	 */
	public String extract(String jsonPath) {
		try {
			Object result = JsonPath.read(jsonResponse, jsonPath);
			return result != null ? result.toString() : null;
		} catch (PathNotFoundException e) {
			log.trace("JSONPath '{}' not found in response: {}", jsonPath, e.getMessage());
			return null;
		}
	}

	/**
	 * Extracts a list of values from the JSON response using a JSONPath expression.
	 * @param jsonPath String the JSONPath expression targeting an array (e.g., "$.items[*].name")
	 * @return List&lt;String&gt; the extracted values, or an empty list if not found
	 */
	public List<String> extractList(String jsonPath) {
		try {
			List<Object> results = JsonPath.read(jsonResponse, jsonPath);
			List<String> out = new ArrayList<>();
			for (Object o : results) out.add(o != null ? o.toString() : null);
			return out;
		} catch (PathNotFoundException e) {
			log.trace("JSONPath '{}' not found in response: {}", jsonPath, e.getMessage());
			return Collections.emptyList();
		}
	}

	/**
	 * Returns the value of the named response header, or null if not present.
	 * @param name String the header name (case-insensitive per HTTP spec)
	 * @return String the header value, or null
	 */
	public String getHeader(String name) {
		Header header = httpResponse.getFirstHeader(name);
		return header != null ? header.getValue() : null;
	}

	/**
	 * Returns all response headers as a Map of name to value.
	 * If the same header name appears multiple times, the last value wins.
	 * @return Map&lt;String, String&gt; all response headers
	 */
	public Map<String, String> getAllHeaders() {
		Map<String, String> map = new HashMap<>();
		for (Header h : httpResponse.getAllHeaders()) {
			map.put(h.getName(), h.getValue());
		}
		return map;
	}

	/**
	 * Returns the Content-Type response header value, or null if not present.
	 * @return String the Content-Type header value
	 */
	public String getContentType() {
		return getHeader("Content-Type");
	}

	/**
	 * Returns the value of the named cookie from the Set-Cookie response headers, or null if not present.
	 * @param name String the cookie name
	 * @return String the cookie value, or null
	 */
	public String getCookie(String name) {
		return getCookies().get(name);
	}

	/**
	 * Returns all cookies from Set-Cookie response headers as a Map of name to value.
	 * @return Map&lt;String, String&gt; cookie name-value pairs
	 */
	public Map<String, String> getCookies() {
		Map<String, String> map = new HashMap<>();
		for (Header h : httpResponse.getHeaders("Set-Cookie")) {
			String raw = h.getValue().split(";")[0];
			int eq = raw.indexOf('=');
			if (eq > 0) map.put(raw.substring(0, eq).trim(), raw.substring(eq + 1).trim());
		}
		return map;
	}

	/**
	 * Returns a formatted string containing the response status, all headers, and the body.
	 * @return String the formatted response dump
	 */
	public String dump() {
		StringBuilder sb = new StringBuilder("Status: ").append(getResponseCode())
				.append(System.lineSeparator()).append("Headers:").append(System.lineSeparator());
		for (Header h : httpResponse.getAllHeaders()) {
			sb.append("  ").append(h.getName()).append(": ").append(h.getValue()).append(System.lineSeparator());
		}
		sb.append("Body:").append(System.lineSeparator()).append(jsonResponse);
		return sb.toString();
	}

	/**
	 * Validates the JSON response body against a JSON Schema file loaded from the classpath.
	 * @param schemaFilePath String the path relative to the classpath root (e.g., "schemas/pet.json")
	 * @return List&lt;String&gt; a list of validation error messages; empty if validation passes
	 */
	public List<String> validateSchema(String schemaFilePath) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			java.io.InputStream schemaStream = getClass().getClassLoader().getResourceAsStream(schemaFilePath);
			if (schemaStream == null) {
				throw new io.github.sentinel.exceptions.IOException("Schema not found on classpath: " + schemaFilePath);
			}
			JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schemaStream);
			JsonNode node = mapper.readTree(jsonResponse);
			Set<ValidationMessage> errors = schema.validate(node);
			List<String> messages = new ArrayList<>();
			for (ValidationMessage vm : errors) messages.add(vm.getMessage());
			return messages;
		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			throw new io.github.sentinel.exceptions.IOException("Failed to parse response JSON for schema validation", e);
		}
	}

	/**
	 * Returns true if the response code is in the 2xx success range.
	 * @return boolean true if successful
	 */
	public boolean isSuccessful() {
		return getResponseCode() >= 200 && getResponseCode() < 300;
	}

	/**
	 * Returns true if the response code is in the 4xx client error range.
	 * @return boolean true if client error
	 */
	public boolean isClientError() {
		return getResponseCode() >= 400 && getResponseCode() < 500;
	}

	/**
	 * Returns true if the response code is in the 5xx server error range.
	 * @return boolean true if server error
	 */
	public boolean isServerError() {
		return getResponseCode() >= 500;
	}
}
