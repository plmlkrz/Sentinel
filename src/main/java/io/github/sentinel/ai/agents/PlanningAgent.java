package io.github.sentinel.ai.agents;

import io.github.sentinel.ai.ClaudeClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI agent that plans test scenarios for a given page URL and test objective.
 * Calls Claude to produce a structured list of scenario outlines following
 * the Sentinel/Cucumber Gherkin step vocabulary.
 */
public class PlanningAgent {

    private static final Logger log = LogManager.getLogger(PlanningAgent.class);

    private static final String SYSTEM_PROMPT =
        "You are a QA automation planning specialist working with the Sentinel BDD framework.\n" +
        "Sentinel uses Cucumber 7 with Gherkin, Selenium 4, Java 17, and YAML-driven page objects.\n" +
        "Your task is to produce a structured list of test scenario titles and step outlines.\n" +
        "Rules:\n" +
        "- Output ONLY a numbered list. No prose, no markdown headers, no code blocks.\n" +
        "- Each item is one scenario: a title line then 3-6 step lines (Given/When/Then/And).\n" +
        "- Use the step vocabulary from the examples provided.\n" +
        "- Separate each scenario with a blank line.\n" +
        "- Do not generate full Gherkin syntax (no Feature:, no @tags, no Scenario: keyword).\n" +
        "- Focus on realistic, meaningful test cases covering happy path and common error cases.";

    private PlanningAgent() {}

    /**
     * Generates a structured list of test scenario outlines for a given page URL
     * and test objective.
     *
     * @param pageUrl       the URL of the page under test
     * @param testObjective a natural-language description of what to test
     * @return a list of scenario outlines, one scenario per entry (title + step lines)
     */
    public static List<String> generatePlan(String pageUrl, String testObjective) {
        log.info("Planning tests for: {} | Objective: {}", pageUrl, testObjective);

        String userPrompt =
            "Page URL: " + pageUrl + "\n\n" +
            "Test Objective: " + testObjective + "\n\n" +
            "Existing step vocabulary reference (use these patterns):\n" +
            "  Given I am on the [Page Name]\n" +
            "  When I fill the account information for account [AccountName] into the [Field] field and the [Field] field\n" +
            "  And I enter [text] in the [Field Name]\n" +
            "  And I click the [Element Name]\n" +
            "  Then I am redirected to the [Page Name]\n" +
            "  And I verify the [Element Name] exists\n" +
            "  And I verify the [Element Name] contains the text \"[text]\"\n" +
            "  And I verify the [Element Name] does not exist\n" +
            "  And I verify the [Element Name] is enabled\n" +
            "  And I verify the [Element Name] is disabled\n\n" +
            "Generate a list of 4-8 test scenario titles with step outlines for this page.";

        String response = ClaudeClient.complete(SYSTEM_PROMPT, userPrompt);
        List<String> scenarios = parseScenarios(response);
        log.info("Planning complete — {} scenario(s) generated", scenarios.size());
        return scenarios;
    }

    private static List<String> parseScenarios(String raw) {
        return Arrays.stream(raw.split("\n\n"))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .collect(Collectors.toList());
    }
}
