package io.github.sentinel.ai.agents;

import io.github.sentinel.ai.ClaudeClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * AI agent that generates Cucumber feature files and Sentinel YAML page objects
 * from a list of scenario outlines produced by the {@link PlanningAgent}.
 * Both files are written to disk immediately upon generation.
 */
public class ScriptGenerationAgent {

    private static final Logger log = LogManager.getLogger(ScriptGenerationAgent.class);

    private static final String FEATURES_DIR = "src/test/java/features/";
    private static final String PAGE_OBJECTS_BASE = "src/test/java/";

    // ── Feature file generation ──────────────────────────────────────────────

    private static final String FEATURE_SYSTEM_PROMPT =
        "You are a QA automation engineer writing Cucumber 7 Gherkin feature files for the Sentinel framework.\n" +
        "You must follow the EXACT style of the example provided. Rules:\n" +
        "- First line: #language: en\n" +
        "- Second line: @[featureNumber]\n" +
        "- Third line: Feature: [featureNumber] [Feature Name]\n" +
        "- Optional: 2-3 line As a/I need/so that user story block (2-space indent)\n" +
        "- Each scenario tagged @[featureNumber][A/B/C...] on the line before Scenario:\n" +
        "- Scenario label: Scenario: [featureNumber][A/B/C...] [Scenario Title]\n" +
        "- Steps: 4-space indent on Given/When/Then, 6-space indent on And\n" +
        "- Output ONLY the raw Gherkin text. No markdown fences. No explanation.";

    // ── YAML page object generation ──────────────────────────────────────────

    private static final String YAML_SYSTEM_PROMPT =
        "You are a QA automation engineer writing Sentinel YAML page object files.\n" +
        "You must follow the EXACT style of the example provided. Rules:\n" +
        "- Top-level keys: urls, accounts (only if logins are referenced in steps), elements\n" +
        "- urls.default is the page's base URL\n" +
        "- Each element key is snake_case matching the element name used in Gherkin steps\n" +
        "- Each element has elementType and at least 2 selectors (prefer id + xpath, or css + xpath)\n" +
        "- Valid elementTypes: Textbox, Button, Div, Link, Checkbox, Dropdown, Table\n" +
        "- Valid selector keys: id, xpath, css, name, class, partialtext\n" +
        "- Output ONLY valid YAML text. No markdown fences. No explanation.";

    private ScriptGenerationAgent() {}

    /**
     * Generates and writes a .feature file and a YAML page object file to disk.
     *
     * @param scenarioOutlines list of scenario outlines from {@link PlanningAgent}
     * @param pageName         page name used to name files (e.g. "LoginPage")
     * @param pageUrl          default URL written into the YAML urls section
     * @param featureNumber    numeric tag prefix (e.g. "500")
     * @param packagePath      subdirectory under src/test/java/ for the YAML (e.g. "com/example/"),
     *                         or null to use "pages/"
     * @return the two generated files
     */
    public static GeneratedFiles generate(
            List<String> scenarioOutlines,
            String pageName,
            String pageUrl,
            String featureNumber,
            String packagePath) throws IOException {

        String resolvedPackage = (packagePath != null && !packagePath.isBlank()) ? packagePath : "pages/";

        String featurePath = FEATURES_DIR + featureNumber + " " + pageName + ".feature";
        String yamlPath = PAGE_OBJECTS_BASE + resolvedPackage + pageName + ".yml";

        log.info("Generating feature file: {}", featurePath);
        String featureContent = generateFeatureContent(scenarioOutlines, pageName, featureNumber);
        File featureFile = writeFile(featurePath, featureContent);

        log.info("Generating YAML page object: {}", yamlPath);
        String yamlContent = generateYamlContent(scenarioOutlines, pageName, pageUrl);
        File yamlFile = writeFile(yamlPath, yamlContent);

        log.info("Script generation complete — {} and {}", featureFile.getName(), yamlFile.getName());
        return new GeneratedFiles(featureFile, yamlFile);
    }

    private static String generateFeatureContent(
            List<String> outlines, String pageName, String featureNumber) {

        String userPrompt =
            "Feature number: " + featureNumber + "\n" +
            "Page name: " + pageName + "\n\n" +
            "EXAMPLE — follow this exact format:\n" +
            "#language: en\n" +
            "@47\n" +
            "Feature: 47 Swag Labs Login\n" +
            "  As a Swag Labs customer who is not locked out,\n" +
            "  I need to be able to log in,\n" +
            "  so that I can purchase Sauce Labs merch.\n\n" +
            "  @47A\n" +
            "  Scenario: 47A Successful Login\n" +
            "    Given I am on the Sauce Demo Login Page\n" +
            "    When I fill the account information for account StandardUser into the Username field and the Password field\n" +
            "      And I click the Login Button\n" +
            "    Then I am redirected to the Sauce Demo Main Page\n" +
            "      And I verify the App Logo exists\n\n" +
            "  @47B\n" +
            "  Scenario: 47B Failed Login with Locked Out User\n" +
            "    Given I am on the Sauce Demo Login Page\n" +
            "    When I fill the account information for account LockedOutUser into the Username field and the Password field\n" +
            "      And I click the Login Button\n" +
            "    Then I verify the Error Message contains the text \"Sorry, this user has been locked out.\"\n\n" +
            "Scenario outlines to implement (generate full Gherkin for each):\n\n" +
            String.join("\n\n", outlines);

        return ClaudeClient.complete(FEATURE_SYSTEM_PROMPT, userPrompt, 6000L);
    }

    private static String generateYamlContent(
            List<String> outlines, String pageName, String pageUrl) {

        String userPrompt =
            "Page name: " + pageName + "\n" +
            "Page URL: " + pageUrl + "\n\n" +
            "EXAMPLE — follow this exact format:\n" +
            "urls:\n" +
            "   default: https://www.saucedemo.com/\n" +
            "accounts:\n" +
            "   default:\n" +
            "      StandardUser:\n" +
            "         username: standard_user\n" +
            "         password: secret_sauce\n" +
            "elements:\n" +
            "  username_field:\n" +
            "    elementType: Textbox\n" +
            "    id: user-name\n" +
            "    xpath: \"//input[@data-test='username']\"\n" +
            "  password_field:\n" +
            "    elementType: Textbox\n" +
            "    css: \"#password\"\n" +
            "    id: \"password\"\n" +
            "  login_button:\n" +
            "    id: \"login-button\"\n" +
            "    xpath: \"//input[@value='LOGIN']\"\n" +
            "  error_message:\n" +
            "    xpath: \"//h3[@data-test='error']\"\n\n" +
            "Infer all elements referenced in these scenario steps and define them:\n\n" +
            String.join("\n\n", outlines);

        return ClaudeClient.complete(YAML_SYSTEM_PROMPT, userPrompt, 4000L);
    }

    private static File writeFile(String filePath, String content) throws IOException {
        var path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
        log.debug("Written: {}", path.toAbsolutePath());
        return path.toFile();
    }

    /**
     * Holds the two files generated by {@link #generate}.
     */
    public record GeneratedFiles(File featureFile, File yamlFile) {}
}
