package io.github.sentinel.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import io.github.sentinel.configurations.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Thread-safe wrapper around the Anthropic Java SDK.
 * Reads the API key from Configuration (sentinel.yml key: anthropicApiKey).
 * The SDK client is lazily initialized on first use and shared for all subsequent calls.
 */
public class ClaudeClient {

    private static final Logger log = LogManager.getLogger(ClaudeClient.class);
    private static final String MODEL = "claude-sonnet-4-6";
    private static final long DEFAULT_MAX_TOKENS = 4096L;

    private static volatile AnthropicClient client = null;

    private ClaudeClient() {}

    private static AnthropicClient getClient() {
        if (client == null) {
            synchronized (ClaudeClient.class) {
                if (client == null) {
                    String apiKey = Configuration.toString("anthropicApiKey");
                    if (apiKey == null || apiKey.isBlank()) {
                        throw new IllegalStateException(
                            "anthropicApiKey not set in conf/sentinel.yml or as -DanthropicApiKey=");
                    }
                    client = AnthropicOkHttpClient.builder()
                        .apiKey(apiKey)
                        .build();
                    log.debug("Anthropic SDK client initialized with model {}", MODEL);
                }
            }
        }
        return client;
    }

    /**
     * Sends a single-turn prompt to Claude and returns the text response.
     *
     * @param systemPrompt the system-level instruction
     * @param userPrompt   the user turn content
     * @return the raw text from Claude's first content block
     */
    public static String complete(String systemPrompt, String userPrompt) {
        return complete(systemPrompt, userPrompt, DEFAULT_MAX_TOKENS);
    }

    /**
     * Sends a single-turn prompt to Claude with a configurable token limit.
     *
     * @param systemPrompt the system-level instruction
     * @param userPrompt   the user turn content
     * @param maxTokens    maximum tokens in the response
     * @return the raw text from Claude's first content block
     */
    public static String complete(String systemPrompt, String userPrompt, long maxTokens) {
        log.debug("Sending request to Claude (model={}, maxTokens={})", MODEL, maxTokens);
        MessageCreateParams params = MessageCreateParams.builder()
            .model(Model.of(MODEL))
            .maxTokens(maxTokens)
            .system(systemPrompt)
            .addUserMessage(userPrompt)
            .build();
        Message message = getClient().messages().create(params);
        if (message.content().isEmpty()) {
            throw new IllegalStateException("Claude returned an empty content block for prompt: " + userPrompt);
        }
        String response = message.content().get(0).asText().text();
        log.debug("Claude response received ({} chars)", response.length());
        return response;
    }
}
