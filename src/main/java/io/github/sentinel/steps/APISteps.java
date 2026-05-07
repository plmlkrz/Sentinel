package io.github.sentinel.steps;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.sentinel.apis.APIManager;
import io.github.sentinel.apis.Response;
import io.github.sentinel.configurations.Configuration;
import io.github.sentinel.enums.RequestType;
import io.github.sentinel.strings.SentinelStringUtils;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class APISteps {
	private static final Logger log = LogManager.getLogger(APISteps.class.getName()); // Create a logger.
	
	/**
	 * Loads an API based on the environment you are currently testing.
     * Refer to the documentation in the sentinel.example project for more information. 
     * <p>
     * <b>Gherkin Examples:</b>
     * <ul>
     * <li>I use the API named Agify API</li>
     * <li>I use the API named My API</li>
     * </ul>
     * <p>
     * 
	 * @param apiName name of the API object we want to use
	 */
	@Given("^I use the API named (.*?)$")
	public static void setAPI(String apiName) {
        APIManager.setAPI(apiName);
	}

	/**
	 * Sets the body of the active API call to the string passed.
	 * 
	 * @param body String the json to be passed as the body of the request.
	 */
	@When("I set the request body to")
	public static void setRequestBody(String body) {
		APIManager.setBody(SentinelStringUtils.replaceStoredVariables(body));
        log.trace("Body passed: {}", body);
	}

	@When("^I set the request body to upload a file from the location (.*?) as a multipart/form-data with the name (.*?)")
	public static void setRequestBodyToMultipartFormDataForFileUpload(String fileToUploadPath, String multipartSegmentName) throws FileNotFoundException {
		Path filePath = Path.of(fileToUploadPath);
		String filename = filePath.getFileName().toString();
		BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(fileToUploadPath));
		String boundary = RandomStringUtils.random(32, 0, 0, true, true, null, new SecureRandom());
		APIManager.setMultipartFormDataBody(multipartSegmentName, boundary, inputStream, filename);
	}

	/**
	 * Loads the indicated testdata located in the API object yaml file to use
	 * as the json for the body of the request.
	 * 
	 * @param testdataName String the name of the testdata entry to use
	 */
	@Given("^I load (.*?) to use as the request body$")
	public static void loadRequestBody(String testdataName) {
		String body = Configuration.getTestData(testdataName, "json");
        APIManager.setBody(body);
        log.trace("Body passed: {}", body);
	}
	
	/**
	 * Sets a query string parameter.
	 * 
	 * <p>
     * <b>Gherkin Examples:</b>
     * <ul>
     * <li>I add a status parameter with the value available</li>
     * <li>I add a name parameter with the value Bob</li>
     * <li>I add an address parameter with the value 143 Down Street</li>
     * </ul>
     * <p>
     * 
	 * @param parameter String the parameter to set
	 * @param value String the value to set it
	 */
	@When("^I add an? (.*?) parameter with the value (.*?)$")
	public static void addParameter(String parameter, String value) {
		APIManager.addParameter(parameter, SentinelStringUtils.replaceStoredVariables(value));

	}
	
	/**
	 * Sends a DELETE, GET, POST or PUT request to the specified endpoint.
	 * <p>
     * <b>Gherkin Examples:</b>
     * <ul>
     * <li>I send a GET request to the pet/findByStatus endpoint</li>
     * <li>I send a POST request to the users endpoint</li>
     * <li>I send a PUT request to the amdins endpoint</li>
     * </ul>
     * <p>
     *  
	 * @param apiCallType
	 * @param endpoint
	 */
	@When("^I send a (DELETE|GET|HEAD|OPTIONS|PATCH|POST|PUT) request to the (.*?) endpoint$")
	public static void sendRequest(String apiCallType, String endpoint) {
		APIManager.sendRequest(RequestType.valueOf(apiCallType), SentinelStringUtils.replaceStoredVariables(endpoint));
	}
	
	/**
	 * Sends a GET or DELETE to the specified endpoint for the indicated record.
	 * 
	 * <p>
     * <b>Gherkin Examples:</b>
     * <ul>
     * <li>I GET record 10 from the pet endpoint</li>
     * <li>I DELETE record bob from the user endpoint</li>
     * </ul>
     * <p>
     *  
	 * @param apiCallType String the type of call to make
	 * @param parameter String the value to send to the endpoint
	 * @param endpoint String the endpoint name as referenced in the swagger file
	 */
	@When("^I (DELETE|GET) record (.*) from the (.*?) endpoint$")
	public static void sendRequest(String apiCallType, String parameter, String endpoint) {
		sendRequest(apiCallType, endpoint + "/" + SentinelStringUtils.replaceVariable(parameter));
	}
	
	/**
	 * Verify that the response code returned from the last API call is what we expect.
	 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Status
	 * 
	 * @param statusCode int the status code expected
	 */
	@When("^I verify the response code equals (\\d{3})$")
	public static void verifyResponseCodeEquals(int statusCode) {
		Response response = APIManager.getResponse();
		int responseCode = response.getResponseCode();
		var expectedResult = SentinelStringUtils.format("Expected the response code to be {}, and it was {}.\nFull response:\n{}",
				statusCode, responseCode, response.getResponse());
		assertTrue(expectedResult, statusCode == responseCode);
	}
	
	@When("^I verify the response was received in less than (\\d{1,2}(?:[.,]\\d{1,4})?) seconds?$")
	public static void verifyResponseTime(double time) {
		Duration timeLimit = Duration.ofMillis((long) (time * 1000));
		Duration responseTime = APIManager.getResponse().getResponseTime();
		
		String expectedResult = SentinelStringUtils.format("Expected the response to take less than {} seconds, but the response took {} ",
				time, responseTime);
		assertTrue(expectedResult, responseTime.compareTo(timeLimit) < 0);
	}

	/**
	 * Validates text in an API response.
	 * 
	 * @param assertion String null to see if the text exists, "does not" to see if it is absent
	 * @param matchType String use "contains" for a partial match otherwise it will be an exact match
	 * @param text String the text to match
	 */
	@Then("^I validate the response( does not)? (has|have|contains?) the text \"([^\"]*)\"$")
    public static void verifyResponseContains(String assertion, String matchType, String text) {
        boolean negate = !StringUtils.isEmpty(assertion);
        boolean partialMatch = matchType.contains("contain");

        int responseCode = APIManager.getResponse().getResponseCode();
        String responseText = APIManager.getResponse().getResponse();
        String expectedResult = SentinelStringUtils.format(
                "Expected the response to {}{} the text {}. The response had a response code of {} and contained the text: {}",
                (negate ? "not " : ""), (partialMatch ? "contain" : "exactly match"), text, responseCode, responseText
                        .replace("\n", " "));
        log.trace(expectedResult);
        if (partialMatch) {
            if (negate) {
                assertFalse(expectedResult, responseText.contains(text));
            } else {
                assertTrue(expectedResult, responseText.contains(text));
            }
        } else {
            if (negate) {
                assertFalse(expectedResult, StringUtils.equals(responseText, text));
            } else {
                assertTrue(expectedResult, StringUtils.equals(responseText, text));
            }
        }
    }

	/**
	 * Adds header into API request
	 *
	 * @param name String name of a header
	 * @param value String value of the header
	 */
	@When("^I add an? (.*?) header with the value (.*?)$")
	public static void addHeader(String name, String value) {
		APIManager.addHeader(name, SentinelStringUtils.replaceStoredVariables(value));
	}

	/**
	 * Adds the parsed string keys and values in Configuration for later use
	 *
	 * @param values String the string with keys and values
	 * Example:
	 *  When I initialize the configuration values as follows
	 *     """
	 *     id: 10
	 *     category_name: puppies
	 *     """
	 *
	 */
	@When("I initialize the configuration values as follows")
	public void iInitializeTheData(String values) {
		var items = values.replace(" ", "").split("\n");
		for (var item : items) {
			int colonIdx = item.indexOf(':');
			if (colonIdx < 0) {
				log.warn("Skipping malformed config entry (no colon found): {}", item);
				continue;
			}
			Configuration.update(item.substring(0, colonIdx), item.substring(colonIdx + 1));
		}
	}

	// ── Authentication ────────────────────────────────────────────────────────

	/**
	 * Sets Basic authentication credentials for the current request.
	 * <p>
	 * <b>Gherkin Examples:</b>
	 * <ul>
	 * <li>I set basic authentication with username admin and password secret</li>
	 * </ul>
	 * @param username String the username
	 * @param password String the password
	 */
	@Given("^I set basic authentication with username (.*?) and password (.*?)$")
	public static void setBasicAuthentication(String username, String password) {
		APIManager.setBasicAuth(username, password);
	}

	/**
	 * Sets a Bearer token for the current request.
	 * <p>
	 * <b>Gherkin Examples:</b>
	 * <ul>
	 * <li>I set the bearer token to eyJhbGciOiJIUzI1NiJ9</li>
	 * </ul>
	 * @param token String the bearer token value
	 */
	@Given("^I set the bearer token to (.*?)$")
	public static void setBearerToken(String token) {
		APIManager.setBearerToken(SentinelStringUtils.replaceStoredVariables(token));
	}

	/**
	 * Sets an API key header for the current request.
	 * <p>
	 * <b>Gherkin Examples:</b>
	 * <ul>
	 * <li>I set the X-API-Key API key header to abc123</li>
	 * </ul>
	 * @param key String the header name
	 * @param value String the header value
	 */
	@Given("^I set the (.*?) API key header to (.*?)$")
	public static void setApiKeyHeader(String key, String value) {
		APIManager.setApiKeyHeader(key, SentinelStringUtils.replaceStoredVariables(value));
	}

	// ── JSONPath extraction and assertions ────────────────────────────────────

	/**
	 * Stores a value extracted from the response JSON into a named configuration variable
	 * for use in subsequent steps.
	 * <p>
	 * <b>Gherkin Examples:</b>
	 * <ul>
	 * <li>I store the response field "$.id" as "petId"</li>
	 * </ul>
	 * @param jsonPath String the JSONPath expression
	 * @param variableName String the configuration variable name to store the value under
	 */
	@Then("^I store the response field \"(.*?)\" as \"(.*?)\"$")
	public static void storeResponseField(String jsonPath, String variableName) {
		APIManager.extractFromResponse(variableName, jsonPath);
	}

	/**
	 * Asserts that a response field equals (or does not equal) the expected value.
	 * <p>
	 * <b>Gherkin Examples:</b>
	 * <ul>
	 * <li>the response field "$.status" should equal "available"</li>
	 * <li>the response field "$.status" should not equal "sold"</li>
	 * </ul>
	 * @param jsonPath String the JSONPath expression
	 * @param negation String null for positive assertion, " not" for negative
	 * @param expected String the expected value
	 */
	@Then("^the response field \"(.*?)\" should( not)? equal \"(.*?)\"$")
	public static void verifyResponseFieldEquals(String jsonPath, String negation, String expected) {
		boolean negate = !StringUtils.isEmpty(negation);
		String actual = APIManager.getResponse().extract(jsonPath);
		String msg = SentinelStringUtils.format("Expected field {} to{} equal {}. Was: {}",
				jsonPath, negate ? " not" : "", expected, actual);
		if (negate) assertFalse(msg, expected.equals(actual));
		else        assertTrue(msg, expected.equals(actual));
	}

	/**
	 * Asserts that a response field contains (or does not contain) the expected substring.
	 * <p>
	 * <b>Gherkin Examples:</b>
	 * <ul>
	 * <li>the response field "$.name" should contain "dog"</li>
	 * </ul>
	 * @param jsonPath String the JSONPath expression
	 * @param negation String null for positive assertion, " not" for negative
	 * @param expected String the expected substring
	 */
	@Then("^the response field \"(.*?)\" should( not)? contain \"(.*?)\"$")
	public static void verifyResponseFieldContains(String jsonPath, String negation, String expected) {
		boolean negate = !StringUtils.isEmpty(negation);
		String actual = APIManager.getResponse().extract(jsonPath);
		boolean contains = actual != null && actual.contains(expected);
		String msg = SentinelStringUtils.format("Expected field {} to{} contain {}. Was: {}",
				jsonPath, negate ? " not" : "", expected, actual);
		if (negate) assertFalse(msg, contains);
		else        assertTrue(msg, contains);
	}

	/**
	 * Asserts that a JSON array at the given path has the expected number of items.
	 * <p>
	 * <b>Gherkin Examples:</b>
	 * <ul>
	 * <li>the response should have 3 items at "$.pets"</li>
	 * </ul>
	 * @param expectedCount int the expected array size
	 * @param jsonPath String the JSONPath expression targeting an array
	 */
	@Then("^the response should have (\\d+) items at \"(.*?)\"$")
	public static void verifyResponseArraySize(int expectedCount, String jsonPath) {
		int actual = APIManager.getResponse().extractList(jsonPath).size();
		String msg = SentinelStringUtils.format("Expected {} items at {}, found {}.", expectedCount, jsonPath, actual);
		assertTrue(msg, expectedCount == actual);
	}

	// ── Response headers ──────────────────────────────────────────────────────

	/**
	 * Asserts that a response header equals (or does not equal) the expected value.
	 * <p>
	 * <b>Gherkin Examples:</b>
	 * <ul>
	 * <li>the response header "Content-Type" should equal "application/json"</li>
	 * </ul>
	 * @param headerName String the header name
	 * @param negation String null for positive assertion, " not" for negative
	 * @param expected String the expected value
	 */
	@Then("^the response header \"(.*?)\" should( not)? equal \"(.*?)\"$")
	public static void verifyResponseHeaderEquals(String headerName, String negation, String expected) {
		boolean negate = !StringUtils.isEmpty(negation);
		String actual = APIManager.getResponseHeader(headerName);
		String msg = SentinelStringUtils.format("Expected header {} to{} equal {}. Was: {}",
				headerName, negate ? " not" : "", expected, actual);
		if (negate) assertFalse(msg, expected.equals(actual));
		else        assertTrue(msg, expected.equals(actual));
	}

	/**
	 * Asserts that a response header contains (or does not contain) the expected substring.
	 * <p>
	 * <b>Gherkin Examples:</b>
	 * <ul>
	 * <li>the response header "Content-Type" should contain "json"</li>
	 * </ul>
	 * @param headerName String the header name
	 * @param negation String null for positive assertion, " not" for negative
	 * @param expected String the expected substring
	 */
	@Then("^the response header \"(.*?)\" should( not)? contain \"(.*?)\"$")
	public static void verifyResponseHeaderContains(String headerName, String negation, String expected) {
		boolean negate = !StringUtils.isEmpty(negation);
		String actual = APIManager.getResponseHeader(headerName);
		boolean contains = actual != null && actual.contains(expected);
		String msg = SentinelStringUtils.format("Expected header {} to{} contain {}. Was: {}",
				headerName, negate ? " not" : "", expected, actual);
		if (negate) assertFalse(msg, contains);
		else        assertTrue(msg, contains);
	}

	// ── JSON Schema validation ────────────────────────────────────────────────

	/**
	 * Validates the last response against a JSON Schema file located in test resources.
	 * <p>
	 * <b>Gherkin Examples:</b>
	 * <ul>
	 * <li>I validate the response matches the schema "schemas/pet.json"</li>
	 * </ul>
	 * @param schemaFilePath String the path to the schema file relative to the classpath root
	 */
	@Then("^I validate the response matches the schema \"(.*?)\"$")
	public static void validateResponseSchema(String schemaFilePath) {
		List<String> errors = APIManager.getResponse().validateSchema(schemaFilePath);
		String msg = SentinelStringUtils.format("Response did not match schema {}. Errors: {}", schemaFilePath, errors);
		assertTrue(msg, errors.isEmpty());
	}

	// ── Request and response logging ─────────────────────────────────────────

	/**
	 * Logs the last request as a curl command at INFO level.
	 * <p>
	 * <b>Gherkin Example:</b>
	 * <ul>
	 * <li>I log the last request as a curl command</li>
	 * </ul>
	 */
	@Then("^I log the last request as a curl command$")
	public static void logLastRequestAsCurl() {
		log.info("Curl command for last request: {}", APIManager.getLastCurlCommand());
	}

	/**
	 * Logs the full last response at INFO level.
	 * <p>
	 * <b>Gherkin Example:</b>
	 * <ul>
	 * <li>I log the last response</li>
	 * </ul>
	 */
	@Then("^I log the last response$")
	public static void logLastResponse() {
		log.info("Response dump:{}{}", System.lineSeparator(), APIManager.getResponse().dump());
	}

	// ── Retry and SSL ─────────────────────────────────────────────────────────

	/**
	 * Sets the retry policy for the next request.
	 * <p>
	 * <b>Gherkin Examples:</b>
	 * <ul>
	 * <li>I set the request to retry up to 3 times with a 500 millisecond delay</li>
	 * </ul>
	 * @param maxRetries int the maximum number of retry attempts
	 * @param delayMs int the delay between retries in milliseconds
	 */
	@Given("^I set the request to retry up to (\\d+) times with a (\\d+) millisecond delay$")
	public static void setRequestRetry(int maxRetries, int delayMs) {
		APIManager.setRetry(maxRetries, (long) delayMs);
	}

	/**
	 * Configures the next request to trust all SSL certificates.
	 * <p>
	 * <b>Gherkin Examples:</b>
	 * <ul>
	 * <li>I allow untrusted SSL certificates</li>
	 * </ul>
	 */
	@Given("^I allow untrusted SSL certificates$")
	public static void allowUntrustedSslCertificates() {
		APIManager.setTrustAllSsl();
	}

	// ── OAuth2 / token management ─────────────────────────────────────────────

	/**
	 * Applies a bearer token stored in a named configuration variable to the current request.
	 * Use after extracting a token with "I store the response field" to chain auth flows.
	 * <p>
	 * <b>Gherkin Example:</b>
	 * <ul>
	 * <li>When I apply the stored bearer token "authToken"</li>
	 * </ul>
	 * @param variableName String the configuration variable holding the token
	 */
	@When("^I apply the stored bearer token \"(.*?)\"$")
	public static void applyStoredBearerToken(String variableName) {
		String token = Configuration.toString(variableName);
		if (token == null || token.isEmpty()) {
			throw new IllegalStateException("No bearer token found for variable '" + variableName + "'. Store it first with: I store the response field \"$.token\" as \"" + variableName + "\"");
		}
		APIManager.setBearerToken(token);
	}

	// ── GraphQL ───────────────────────────────────────────────────────────────

	/**
	 * Sends a GraphQL query to the given endpoint as a POST request.
	 * The docstring body is wrapped automatically in {"query":"..."} format.
	 * <p>
	 * <b>Gherkin Example:</b>
	 * <pre>
	 * When I send a GraphQL query to the /graphql endpoint
	 *   """
	 *   { user(id: 1) { name email } }
	 *   """
	 * </pre>
	 * @param endpoint String the GraphQL endpoint path
	 * @param query String the GraphQL query string (from docstring)
	 */
	@When("^I send a GraphQL query to the (.*?) endpoint$")
	public static void sendGraphQLQuery(String endpoint, String query) {
		APIManager.setGraphQLBody(SentinelStringUtils.replaceStoredVariables(query));
		APIManager.sendRequest(RequestType.POST, SentinelStringUtils.replaceStoredVariables(endpoint));
	}

	// ── Cookies ───────────────────────────────────────────────────────────────

	/**
	 * Adds a cookie to the next request.
	 * <p>
	 * <b>Gherkin Examples:</b>
	 * <ul>
	 * <li>I add a cookie named "session" with the value "abc123"</li>
	 * </ul>
	 * @param name String the cookie name
	 * @param value String the cookie value
	 */
	@Given("^I add a cookie named \"(.*?)\" with the value \"(.*?)\"$")
	public static void addCookie(String name, String value) {
		APIManager.addCookie(name, value);
	}

	/**
	 * Asserts that a response cookie equals (or does not equal) the expected value.
	 * <p>
	 * <b>Gherkin Examples:</b>
	 * <ul>
	 * <li>the response cookie "session" should equal "abc123"</li>
	 * </ul>
	 * @param cookieName String the cookie name
	 * @param negation String null for positive assertion, " not" for negative
	 * @param expected String the expected value
	 */
	@Then("^the response cookie \"(.*?)\" should( not)? equal \"(.*?)\"$")
	public static void verifyResponseCookieEquals(String cookieName, String negation, String expected) {
		boolean negate = !StringUtils.isEmpty(negation);
		String actual = APIManager.getResponseCookie(cookieName);
		String msg = SentinelStringUtils.format("Expected cookie {} to{} equal {}. Was: {}",
				cookieName, negate ? " not" : "", expected, actual);
		if (negate) assertFalse(msg, expected.equals(actual));
		else        assertTrue(msg, expected.equals(actual));
	}
}
