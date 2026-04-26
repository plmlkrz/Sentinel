package io.github.sentinel.apis;

import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.client.utils.URIBuilder;
import io.github.sentinel.configurations.Configuration;
import io.github.sentinel.enums.YAMLObjectType;
import io.github.sentinel.exceptions.IOException;
import io.github.sentinel.system.YAMLObject;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

public class API extends YAMLObject {
	private Request request = new Request();
	
    /**
     * Constructor
     * @param apiName String the exact case-sensitive name of the yaml file containing the API information.
     */
    public API(String apiName) {
    	super(apiName);
    	this.yamlObjectType = YAMLObjectType.API;
    }
		
	/**
	 * Returns a java.net.URI constructed from the URL listed in the API yaml file.
	 * 
	 * @return java.net.URI the constructed URI
	 */
	protected URIBuilder getURIBuilder(String passedText) throws URISyntaxException {
		String swaggerUrl = Configuration.getURL(APIManager.getAPI());
		SwaggerParseResult result = new OpenAPIParser().readLocation(swaggerUrl, null, null);
		OpenAPI openAPI = result.getOpenAPI();
		List<Server> servers = (openAPI != null) ? openAPI.getServers() : null;

		if (servers != null && !servers.isEmpty()) {
			try {
				var firstServer = servers.get(0).getUrl();
				var uriBuilder = new URIBuilder(firstServer + passedText);
				if (!uriBuilder.isAbsolute()) {
					String basePath = firstServer.endsWith("/") ? firstServer.substring(0, firstServer.length() - 1) : firstServer;
					uriBuilder = new URIBuilder(swaggerUrl).setPath(basePath + passedText);
				}
				return uriBuilder;
			} catch (URISyntaxException e) {
				throw new IOException(e);
			}
		}

		// Fall back: treat the configured URL as a plain base URL
		String base = swaggerUrl.endsWith("/") ? swaggerUrl.substring(0, swaggerUrl.length() - 1) : swaggerUrl;
		return new URIBuilder(base + passedText);
	}
	
	public Request getRequest() {
		return request; 
	}

}
