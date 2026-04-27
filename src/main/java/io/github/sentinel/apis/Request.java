package io.github.sentinel.apis;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import io.github.sentinel.enums.HttpBodyType;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.sentinel.enums.RequestType;
import io.github.sentinel.exceptions.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Request {
	private static final Logger log = LogManager.getLogger(Request.class.getName());

	protected HttpRequestBase httpRequest = null;
	protected List<NameValuePair> parameters = new ArrayList<>();
	protected List<NameValuePair> headers = new ArrayList<>();
	protected HttpEntity body = null;
	protected HttpBodyType bodyType = null;
	private String rawBody = null;
	private int maxRetries = 0;
	private long retryDelayMs = 1000;
	private boolean trustAllSsl = false;
	private List<BasicClientCookie> cookies = new ArrayList<>();

	/**
	 * Set a parameter and its value for a request. They will show up as part of the query string in the API request.
	 * @param parameter String the parameter being passed
	 * @param value String the value to be passed
	 */
	public void addParameter(String parameter, String value) {
		parameters.add(new BasicNameValuePair(parameter, value));
	}

	/**
	 * Returns a URI that includes any parameters if it is set. Otherwise it returns the existing URI.
	 * @return HttpRequestBase the new request object
	 */
	protected HttpRequestBase buildURI() {
		if (!parameters.isEmpty()) {
			URI uri;
			try {
				uri = new URIBuilder(httpRequest.getURI())
						.addParameters(parameters)
						.build();
				httpRequest.setURI(uri);
			} catch (URISyntaxException e) {
				throw new IOException(e);
			}
		}

		log.trace("URI Constructed: {}", httpRequest.getURI());
		return httpRequest;
	}

	protected void setHeaders() {
		for (NameValuePair h : headers) {
			httpRequest.setHeader(h.getName(), h.getValue());
		}
	}

	/**
	 * Set a header and its value for a request.
	 * @param name String the name being passed
	 * @param value String the value to be passed
	 */
	public void addHeader(String name, String value) {
		headers.add(new BasicNameValuePair(name, value));
	}

	/**
	 * Sets the Authorization header to Basic authentication using the given credentials.
	 * @param username String the username
	 * @param password String the password
	 */
	public void setBasicAuth(String username, String password) {
		String encoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(UTF_8));
		addHeader("Authorization", "Basic " + encoded);
	}

	/**
	 * Sets the Authorization header to Bearer token authentication.
	 * @param token String the bearer token value
	 */
	public void setBearerToken(String token) {
		addHeader("Authorization", "Bearer " + token);
	}

	/**
	 * Sets an arbitrary API key header.
	 * @param key String the header name (e.g., "X-API-Key")
	 * @param value String the header value
	 */
	public void setApiKeyHeader(String key, String value) {
		addHeader(key, value);
	}

	/**
	 * Sets the retry policy for subsequent requests.
	 * @param maxRetries int the maximum number of retry attempts (0 = no retries)
	 * @param delayMs long the delay in milliseconds between retries
	 */
	public void setRetry(int maxRetries, long delayMs) {
		this.maxRetries = maxRetries;
		this.retryDelayMs = delayMs;
	}

	/**
	 * Configures the HTTP client to trust all SSL certificates, including self-signed ones.
	 * Use only in test environments.
	 */
	public void setTrustAllSsl() {
		this.trustAllSsl = true;
	}

	/**
	 * Adds a cookie to be sent with the next request.
	 * @param name String the cookie name
	 * @param value String the cookie value
	 */
	public void addCookie(String name, String value) {
		BasicClientCookie cookie = new BasicClientCookie(name, value);
		cookie.setPath("/");
		cookies.add(cookie);
	}

	/**
	 * Builds a curl command string representing the current request state.
	 * Must be called after buildURI() and setHeaders() have been applied.
	 * @return String the curl command, or an empty string if no request has been initialized
	 */
	public String toCurlCommand() {
		if (httpRequest == null) return "";
		StringBuilder curl = new StringBuilder("curl -X ").append(httpRequest.getMethod());
		for (Header h : httpRequest.getAllHeaders()) {
			curl.append(" -H '").append(h.getName()).append(": ").append(h.getValue()).append("'");
		}
		if (rawBody != null) {
			curl.append(" -d '").append(rawBody.replace("'", "'\\''")).append("'");
		}
		curl.append(" '").append(httpRequest.getURI()).append("'");
		return curl.toString();
	}

	/**
	 * Builds and sets a GraphQL request body from a query string.
	 * Wraps the query in the standard {"query":"..."} JSON envelope and sets Content-Type to application/json.
	 * @param query String the GraphQL query or mutation string
	 */
	public void setGraphQLBody(String query) {
		String escaped = query.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
		setBody("{\"query\":\"" + escaped + "\"}");
		boolean hasContentType = headers.stream().anyMatch(h -> "Content-Type".equalsIgnoreCase(h.getName()));
		if (!hasContentType) {
			addHeader("Content-Type", "application/json");
		}
	}

	/**
	 * Creates a StringEntity to hold the json body.
	 * @param body String the JSON to encode.
	 */
	public void setBody(String body) {
		try {
			this.rawBody = body;
			this.body = new StringEntity(body);
			bodyType = HttpBodyType.STRING;
		} catch (UnsupportedEncodingException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Sets the body to a multipart/form-data body, using the given name, boundary, input stream, and filename.
	 * Also sets a header with the Content-Type set to 'multipart/form-data' and specifies the boundary string passed to this method.
	 * @param nameOfInput String name of the multipart segment.
	 * @param boundary String the multipart boundary.
	 * @param inputStream InputStream input stream of the file to upload.
	 * @param filename String name of the file being uploaded.
	 */
	public void setMultipartFormDataBody(String nameOfInput, String boundary, InputStream inputStream, String filename) {
		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		entityBuilder.setBoundary(boundary)
				.setMode(HttpMultipartMode.STRICT)
				.setCharset(UTF_8);
		entityBuilder.addBinaryBody(nameOfInput, inputStream, ContentType.MULTIPART_FORM_DATA, filename);
		body = entityBuilder.build();
		bodyType = HttpBodyType.MULTIPARTFORMDATA;
		addHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
	}

	/**
	 * Construct a request, send it to the active API, and store the response for retrieval.
	 * @param type io.github.sentinel.enums.RequestType the type of request to send
	 * @param endpoint the endpoint to send the request
	 */
	public void createAndSendRequest(RequestType type, String endpoint) {
		endpoint = StringUtils.prependIfMissing(endpoint, "/");
		try {
			switch (type) {
			case DELETE:
				httpRequest = new HttpDelete(APIManager.getAPI().getURIBuilder(endpoint).build());
				break;
			case GET:
				httpRequest = new HttpGet(APIManager.getAPI().getURIBuilder(endpoint).build());
				break;
			case HEAD:
				httpRequest = new HttpHead(APIManager.getAPI().getURIBuilder(endpoint).build());
				break;
			case OPTIONS:
				httpRequest = new HttpOptions(APIManager.getAPI().getURIBuilder(endpoint).build());
				break;
			case PATCH:
				httpRequest = new HttpPatch(APIManager.getAPI().getURIBuilder(endpoint).build());
				((HttpEntityEnclosingRequestBase) httpRequest).setEntity(body);
				break;
			case POST:
				httpRequest = new HttpPost(APIManager.getAPI().getURIBuilder(endpoint).build());
				((HttpEntityEnclosingRequestBase) httpRequest).setEntity(body);
				break;
			case PUT:
				httpRequest = new HttpPut(APIManager.getAPI().getURIBuilder(endpoint).build());
				((HttpEntityEnclosingRequestBase) httpRequest).setEntity(body);
				break;
			}
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}

		if (bodyType == HttpBodyType.MULTIPARTFORMDATA) {
			httpRequest.setConfig(RequestConfig.custom().setExpectContinueEnabled(true).build());
		}

		setHeaders();
		buildURI();
		sendRequest();
	}

	/**
	 * Send the request with retry support, store the response, then reset state.
	 */
	protected void sendRequest() {
		Response response = null;
		int attempt = 0;
		while (true) {
			try {
				try (CloseableHttpClient httpClient = buildHttpClient()) {
					long startTime = System.nanoTime();
					response = new Response(httpClient.execute(httpRequest));
					response.setResponseTime(Duration.ofNanos(System.nanoTime() - startTime));
				}
				if (response.getResponseCode() >= 500 && attempt < maxRetries) {
					attempt++;
					log.warn("Request returned server error {} (attempt {}/{}), retrying in {} ms.",
							response.getResponseCode(), attempt, maxRetries, retryDelayMs);
					sleepForRetry();
					continue;
				}
				break;
			} catch (java.io.IOException e) {
				if (++attempt > maxRetries) throw new IOException(e);
				log.warn("Request failed (attempt {}/{}), retrying in {} ms: {}",
						attempt, maxRetries, retryDelayMs, e.getMessage());
				sleepForRetry();
			}
		}
		APIManager.setLastCurlCommand(toCurlCommand());
		log.trace("Curl: {}", toCurlCommand());
		log.trace("Response Code: {} Response: {}", response.getResponseCode(), response.getResponse());
		APIManager.setResponse(response);
		reset();
	}

	private void sleepForRetry() {
		try {
			Thread.sleep(retryDelayMs);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			throw new IOException(ie);
		}
	}

	private CloseableHttpClient buildHttpClient() {
		BasicCookieStore cookieStore = buildCookieStore();
		if (trustAllSsl) {
			try {
				javax.net.ssl.SSLContext ctx = new SSLContextBuilder()
						.loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
						.build();
				return HttpClients.custom()
						.setSSLContext(ctx)
						.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
						.setDefaultCookieStore(cookieStore)
						.build();
			} catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
				throw new IOException("Failed to build trust-all SSL client", e);
			}
		}
		return HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build();
	}

	private BasicCookieStore buildCookieStore() {
		BasicCookieStore store = new BasicCookieStore();
		String host = (httpRequest != null && httpRequest.getURI() != null)
				? httpRequest.getURI().getHost() : "";
		for (BasicClientCookie c : cookies) {
			if (c.getDomain() == null || c.getDomain().isEmpty()) c.setDomain(host);
			store.addCookie(c);
		}
		return store;
	}

	/**
	 * Reset all values so we can make a new request.
	 */
	protected void reset() {
		parameters.clear();
		body = null;
		rawBody = null;
		httpRequest = null;
		headers.clear();
		maxRetries = 0;
		retryDelayMs = 1000;
		trustAllSsl = false;
		cookies.clear();
	}

}
