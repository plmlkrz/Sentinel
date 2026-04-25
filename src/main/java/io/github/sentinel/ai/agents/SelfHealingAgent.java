package io.github.sentinel.ai.agents;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.sentinel.ai.ClaudeClient;
import io.github.sentinel.enums.SelectorType;
import io.github.sentinel.exceptions.FileException;
import io.github.sentinel.system.FileManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI agent that automatically repairs broken Selenium element selectors.
 * <p>
 * When {@link io.github.sentinel.elements.Element} exhausts all configured selectors,
 * this agent sends the element name, failed selectors, and the current page HTML to Claude,
 * which returns 1-3 alternative selectors. The new selectors are:
 * <ol>
 *   <li>Returned to {@code Element.element()} for an immediate live retry</li>
 *   <li>Written back into the element's YAML page object file on disk</li>
 * </ol>
 * Enable/disable via {@code aiSelfHeal: true/false} in {@code conf/sentinel.yml}.
 */
public class SelfHealingAgent {

    private static final Logger log = LogManager.getLogger(SelfHealingAgent.class);

    private static final int HTML_TRUNCATE_CHARS = 4000;

    private static final String SYSTEM_PROMPT =
        "You are a Selenium self-healing expert. A Selenium element locator has failed.\n" +
        "You will be given: the element name, the selectors that failed, and truncated page HTML.\n" +
        "Your task: suggest 1-3 NEW selectors that are likely to find this element in that HTML.\n" +
        "Output ONLY a list, one selector per line, in this exact format:\n" +
        "  selectorType: selectorValue\n" +
        "Valid selectorTypes: id, xpath, css, name, class\n" +
        "Examples:\n" +
        "  xpath: //button[@data-testid='login-btn']\n" +
        "  css: button.login-button\n" +
        "  id: loginButton\n" +
        "No explanation. No markdown. No extra text. Only the selector lines.";

    private SelfHealingAgent() {}

    /**
     * Attempts to find new selectors for a failed element using Claude.
     * New selectors are injected into the YAML file on disk if found.
     *
     * @param elementName     the name of the failed element (as defined in the YAML)
     * @param pageName        the page name (maps to [pageName].yml)
     * @param failedSelectors the selectors that all failed
     * @param pageHtml        the current page source from {@code driver().getPageSource()}
     * @return a map of new selectors if healing succeeded, empty map otherwise
     */
    public static Map<SelectorType, String> heal(
            String elementName,
            String pageName,
            Map<SelectorType, String> failedSelectors,
            String pageHtml) {

        log.info("Self-healing element '{}' on page '{}'", elementName, pageName);

        String truncatedHtml = pageHtml.length() > HTML_TRUNCATE_CHARS
            ? pageHtml.substring(0, HTML_TRUNCATE_CHARS) + "...[truncated]"
            : pageHtml;

        String failedSelectorsText = failedSelectors.entrySet().stream()
            .map(e -> "  " + e.getKey().name().toLowerCase() + ": " + e.getValue())
            .collect(Collectors.joining("\n"));

        String userPrompt =
            "Element name: " + elementName + "\n" +
            "Page: " + pageName + "\n\n" +
            "Failed selectors (these did NOT find the element):\n" +
            failedSelectorsText + "\n\n" +
            "Current page HTML (truncated):\n" +
            truncatedHtml;

        String response = ClaudeClient.completeWithThinking(SYSTEM_PROMPT, userPrompt, 8192L);
        Map<SelectorType, String> newSelectors = parseSelectors(response);

        if (newSelectors.isEmpty()) {
            log.warn("Self-healing produced no valid selectors for '{}' on '{}'", elementName, pageName);
            return Collections.emptyMap();
        }

        persistToYaml(elementName, pageName, newSelectors);
        return newSelectors;
    }

    private static Map<SelectorType, String> parseSelectors(String raw) {
        Map<SelectorType, String> result = new EnumMap<>(SelectorType.class);
        for (String line : raw.split("\n")) {
            line = line.trim();
            if (line.isBlank()) continue;
            int colon = line.indexOf(':');
            if (colon < 1) continue;
            String typeName = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();
            if (value.isBlank()) continue;
            try {
                result.put(SelectorType.of(typeName), value);
            } catch (IllegalArgumentException e) {
                log.debug("Skipping unrecognised selector type from Claude: '{}'", typeName);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static void persistToYaml(
            String elementName,
            String pageName,
            Map<SelectorType, String> newSelectors) {

        File yamlFile;
        try {
            yamlFile = FileManager.findFilePath(pageName + ".yml");
        } catch (FileException e) {
            log.warn("Self-healer could not locate YAML for page '{}' — selectors NOT persisted: {}",
                pageName, e.getMessage());
            return;
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            Map<String, Object> rawYaml = mapper.readValue(
                yamlFile, new TypeReference<Map<String, Object>>() {});

            Object elementsObj = rawYaml.get("elements");
            if (!(elementsObj instanceof Map)) {
                log.warn("Self-healer: 'elements' section not found or not a map in {}.yml", pageName);
                return;
            }
            Map<String, Object> elements = (Map<String, Object>) elementsObj;

            Object elementObj = elements.get(elementName);
            if (!(elementObj instanceof Map)) {
                log.warn("Self-healer: element '{}' not found in {}.yml elements section",
                    elementName, pageName);
                return;
            }
            Map<String, Object> elementMap = (Map<String, Object>) elementObj;

            newSelectors.forEach((type, value) ->
                elementMap.put(type.name().toLowerCase(), value));

            mapper.writeValue(yamlFile, rawYaml);
            log.info("Self-healer persisted new selectors for '{}' to {}", elementName, yamlFile.getPath());

        } catch (IOException e) {
            log.warn("Self-healer could not write YAML for '{}' — selectors NOT persisted: {}",
                pageName, e.getMessage());
        }
    }
}
