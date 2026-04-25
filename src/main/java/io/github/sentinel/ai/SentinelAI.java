package io.github.sentinel.ai;

import io.github.sentinel.ai.agents.PlanningAgent;
import io.github.sentinel.ai.agents.ScriptGenerationAgent;
import io.github.sentinel.ai.agents.ScriptGenerationAgent.GeneratedFiles;
import io.github.sentinel.ai.agents.SelfHealingAgent;
import io.github.sentinel.configurations.Configuration;
import io.github.sentinel.enums.SelectorType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Facade that coordinates all three Sentinel AI agents.
 *
 * <h2>Interactive usage (engineer-initiated):</h2>
 * <pre>
 * // Plan only — get back scenario outlines:
 * List&lt;String&gt; plan = SentinelAI.plan("https://example.com/login", "test login flows");
 *
 * // Plan then generate feature + YAML files in one call:
 * SentinelAI.planAndGenerate(
 *     "https://example.com/login",   // page URL
 *     "test login flows",            // test objective
 *     "LoginPage",                   // page/file name
 *     "500",                         // feature number tag
 *     "com/example/");               // YAML package path under src/test/java/
 * </pre>
 *
 * <h2>Automatic self-healing:</h2>
 * {@link #selfHeal} is called automatically by
 * {@link io.github.sentinel.elements.Element} when all selectors fail.
 * Engineers do not need to call it directly.
 * Enable/disable via {@code aiSelfHeal: true/false} in {@code conf/sentinel.yml}.
 */
public class SentinelAI {

    private static final Logger log = LogManager.getLogger(SentinelAI.class);

    private SentinelAI() {}

    /**
     * Runs the {@link PlanningAgent} and returns structured scenario outlines.
     *
     * @param pageUrl       the URL of the page under test
     * @param testObjective a natural-language description of what to test
     * @return list of scenario outline strings (title + steps per entry)
     */
    public static List<String> plan(String pageUrl, String testObjective) {
        return PlanningAgent.generatePlan(pageUrl, testObjective);
    }

    /**
     * Runs the {@link PlanningAgent} then the {@link ScriptGenerationAgent} end-to-end.
     * Writes a Gherkin {@code .feature} file and a YAML page object to disk.
     *
     * @param pageUrl       the URL of the page under test
     * @param testObjective a natural-language description of what to test
     * @param pageName      name used for both files (e.g. {@code "LoginPage"})
     * @param featureNumber numeric tag prefix (e.g. {@code "500"})
     * @param packagePath   subdirectory under {@code src/test/java/} for the YAML
     *                      (e.g. {@code "com/example/"}), or {@code null} for {@code pages/}
     * @return the two generated {@link java.io.File} objects
     */
    public static GeneratedFiles planAndGenerate(
            String pageUrl,
            String testObjective,
            String pageName,
            String featureNumber,
            String packagePath) throws IOException {

        List<String> scenarioOutlines = PlanningAgent.generatePlan(pageUrl, testObjective);
        return ScriptGenerationAgent.generate(scenarioOutlines, pageName, pageUrl, featureNumber, packagePath);
    }

    /**
     * Invokes the {@link SelfHealingAgent} when Selenium element resolution has failed.
     * Returns an empty map (and does nothing) when {@code aiSelfHeal} is {@code false}.
     *
     * @param elementName     the element name as defined in the YAML page object
     * @param pageName        the page name (used to locate the YAML file)
     * @param failedSelectors selectors that all failed to find the element
     * @param pageHtml        the current page source from {@code driver().getPageSource()}
     * @return new selectors suggested by Claude, empty map if healing is disabled or failed
     */
    public static Map<SelectorType, String> selfHeal(
            String elementName,
            String pageName,
            Map<SelectorType, String> failedSelectors,
            String pageHtml) {

        if (!Configuration.toBoolean("aiSelfHeal")) {
            return Collections.emptyMap();
        }
        try {
            return SelfHealingAgent.heal(elementName, pageName, failedSelectors, pageHtml);
        } catch (Exception e) {
            log.warn("Self-healing attempt failed for element '{}' on page '{}': {}",
                elementName, pageName, e.getMessage());
            return Collections.emptyMap();
        }
    }
}
