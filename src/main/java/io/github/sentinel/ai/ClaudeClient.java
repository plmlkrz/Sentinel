package io.github.sentinel.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import io.github.sentinel.configurations.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Thread-safe wrapper around the Anthropic Java SDK.
 * Reads the API key from Configuration (sentinel.yml key: anthropicApiKey) or the
 * ANTHROPIC_API_KEY environment variable as a fallback.
 * The SDK client is lazily initialized on first use and shared for all subsequent calls.
 * The model is read from Configuration (sentinel.yml key: aiModel) on every call,
 * defaulting to claude-sonnet-4-6.
 */
public class ClaudeClient {

    private static final Logger log = LogManager.getLogger(ClaudeClient.class);
    private static final String DEFAULT_MODEL = "claude-sonnet-4-6";
    private static final long DEFAULT_MAX_TOKENS = 4096L;

    private static volatile AnthropicClient client = null;

    private ClaudeClient() {}

    private static String getModel() {
        String configured = Configuration.toString("aiModel");
        return (configured != null && !configured.isBlank()) ? configured : DEFAULT_MODEL;
    }

    private static AnthropicClient getClient() {
        if (client == null) {
            synchronized (ClaudeClient.class) {
                if (client == null) {
                    String apiKey = Configuration.toString("anthropicApiKey");
                    if (apiKey != null && !apiKey.isBlank()) {
                        client = AnthropicOkHttpClient.builder()
                            .apiKey(apiKey)
                            .build();
                    } else {
                        // Falls back to ANTHROPIC_API_KEY env var.
                        // fromEnv() throws with a clear message if the env var is also absent.
                        client = AnthropicOkHttpClient.fromEnv();
                    }
                    log.debug("Anthropic SDK client initialized with model {}", getModel());
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
     * @return the raw text from Claude's first text content block
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
     * @return the raw text from Claude's first text content block
     */
    public static String complete(String systemPrompt, String userPrompt, long maxTokens) {
        String model = getModel();
        log.debug("Sending request to Claude (model={}, maxTokens={})", model, maxTokens);

        // Prompt caching: system prompt wrapped with cache_control. The current prompts are
        // ~100-200 tokens — below Sonnet 4.6's 2048-token minimum — so no cache hits yet.
        // Infrastructure is in place for when prompts grow.
        TextBlockParam cachedSystem = TextBlockParam.builder()
            .text(systemPrompt)
            .cacheControl(CacheControlEphemeral.builder().build())
            .build();

        MessageCreateParams params = MessageCreateParams.builder()
            .model(Model.of(model))
            .maxTokens(maxTokens)
            .systemOfTextBlockParams(List.of(cachedSystem))
            .addUserMessage(userPrompt)
            .build();

        Message message = getClient().messages().create(params);

        message.stopReason().ifPresent(reason -> {
            if (StopReason.MAX_TOKENS.equals(reason)) {
                throw new IllegalStateException(
                    "Claude response was cut off (stop_reason=max_tokens, limit=" + maxTokens + "). " +
                    "Increase the token limit for this operation.");
            }
        });

        if (message.content().isEmpty()) {
            throw new IllegalStateException(
                "Claude returned an empty content block for prompt: " + userPrompt);
        }

        // Stream-based extraction is safe when thinking blocks precede the text block.
        String response = message.content().stream()
            .flatMap(block -> block.text().stream())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Claude response contained no text block for prompt: " + userPrompt))
            .text();

        log.debug("Claude response received ({} chars)", response.length());
        return response;
    }

    /**
     * Sends a single-turn prompt to Claude with adaptive thinking enabled.
     * Use for complex reasoning tasks such as HTML analysis in SelfHealingAgent.
     *
     * @param systemPrompt the system-level instruction
     * @param userPrompt   the user turn content
     * @param maxTokens    maximum tokens for thinking + response combined
     * @return the raw text from Claude's first text content block
     */
    public static String completeWithThinking(
            String systemPrompt,
            String userPrompt,
            long maxTokens) {

        String model = getModel();
        log.debug("Sending request to Claude with adaptive thinking (model={}, maxTokens={})",
            model, maxTokens);

        TextBlockParam cachedSystem = TextBlockParam.builder()
            .text(systemPrompt)
            .cacheControl(CacheControlEphemeral.builder().build())
            .build();

        MessageCreateParams params = MessageCreateParams.builder()
            .model(Model.of(model))
            .maxTokens(maxTokens)
            .systemOfTextBlockParams(List.of(cachedSystem))
            .thinking(ThinkingConfigAdaptive.builder().build())
            .addUserMessage(userPrompt)
            .build();

        Message message = getClient().messages().create(params);

        message.stopReason().ifPresent(reason -> {
            if (StopReason.MAX_TOKENS.equals(reason)) {
                throw new IllegalStateException(
                    "Claude thinking response was cut off (stop_reason=max_tokens, limit=" + maxTokens + "). " +
                    "Increase the token limit for this operation.");
            }
        });

        if (message.content().isEmpty()) {
            throw new IllegalStateException(
                "Claude returned an empty content block for prompt: " + userPrompt);
        }

        // Thinking blocks appear before text blocks in the response; stream skips them.
        String response = message.content().stream()
            .flatMap(block -> block.text().stream())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Claude thinking response contained no text block for prompt: " + userPrompt))
            .text();

        log.debug("Claude thinking response received ({} chars)", response.length());
        return response;
    }
}
