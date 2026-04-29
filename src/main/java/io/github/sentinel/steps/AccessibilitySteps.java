package io.github.sentinel.steps;

import static org.junit.Assert.assertTrue;

import java.util.List;

import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.results.Rule;
import com.deque.html.axecore.selenium.AxeBuilder;

import io.cucumber.java.en.Then;
import io.github.sentinel.strings.SentinelStringUtils;
import io.github.sentinel.webdrivers.Driver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Cucumber step definitions for accessibility testing using axe-core.
 * Requires com.deque.html.axe-core:selenium on the classpath.
 * <p>
 * Add to pom.xml (optional dependency):
 * <pre>
 *   &lt;dependency&gt;
 *     &lt;groupId&gt;com.deque.html.axe-core&lt;/groupId&gt;
 *     &lt;artifactId&gt;selenium&lt;/artifactId&gt;
 *     &lt;version&gt;4.9.1&lt;/version&gt;
 *   &lt;/dependency&gt;
 * </pre>
 */
public class AccessibilitySteps {

    private static final Logger log = LogManager.getLogger(AccessibilitySteps.class);

    /**
     * Runs axe-core against the current page and fails if any WCAG violations are found.
     * <p><b>Gherkin Example:</b> Then I verify the page passes accessibility standards
     */
    @Then("^I verify the page passes accessibility standards$")
    public static void verifyPagePassesAccessibility() {
        Results results = new AxeBuilder().analyze(Driver.getWebDriver());
        List<Rule> violations = results.getViolations();
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder("Accessibility violations found:\n");
            for (Rule violation : violations) {
                sb.append("  [").append(violation.getImpact()).append("] ")
                  .append(violation.getId()).append(": ")
                  .append(violation.getDescription()).append("\n");
            }
            log.warn(sb.toString());
        }
        String msg = SentinelStringUtils.format(
                "Expected no accessibility violations, but found {}.", violations.size());
        assertTrue(msg, violations.isEmpty());
    }

    /**
     * Runs axe-core and fails if any violations at or above the specified impact level exist.
     * Valid impact levels: minor, moderate, serious, critical.
     * <p><b>Gherkin Example:</b> Then I verify the page has no serious accessibility violations
     * @param impactLevel String the minimum impact level to check (minor, moderate, serious, critical)
     */
    @Then("^I verify the page has no (minor|moderate|serious|critical) accessibility violations$")
    public static void verifyPagePassesAccessibilityAtLevel(String impactLevel) {
        Results results = new AxeBuilder().analyze(Driver.getWebDriver());
        List<Rule> violations = results.getViolations().stream()
                .filter(v -> impactMeetsThreshold(v.getImpact(), impactLevel))
                .toList();
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder(
                    "Accessibility violations at '" + impactLevel + "' level or above:\n");
            for (Rule v : violations) {
                sb.append("  [").append(v.getImpact()).append("] ")
                  .append(v.getId()).append(": ").append(v.getDescription()).append("\n");
            }
            log.warn(sb.toString());
        }
        String msg = SentinelStringUtils.format(
                "Expected no '{}' or higher accessibility violations, but found {}.",
                impactLevel, violations.size());
        assertTrue(msg, violations.isEmpty());
    }

    /**
     * Logs all axe-core violations without failing the test — useful for audit runs.
     * <p><b>Gherkin Example:</b> Then I log the accessibility violations on the page
     */
    @Then("^I log the accessibility violations on the page$")
    public static void logAccessibilityViolations() {
        Results results = new AxeBuilder().analyze(Driver.getWebDriver());
        List<Rule> violations = results.getViolations();
        if (violations.isEmpty()) {
            log.info("No accessibility violations found.");
        } else {
            log.info("Accessibility violations ({}):", violations.size());
            for (Rule v : violations) {
                log.info("  [{}] {}: {}", v.getImpact(), v.getId(), v.getDescription());
            }
        }
    }

    private static boolean impactMeetsThreshold(String actual, String threshold) {
        int[] levels = {0, 1, 2, 3};
        String[] names = {"minor", "moderate", "serious", "critical"};
        int actualLevel = indexOf(names, actual);
        int thresholdLevel = indexOf(names, threshold);
        return actualLevel >= thresholdLevel;
    }

    private static int indexOf(String[] arr, String val) {
        if (val == null) return 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equalsIgnoreCase(val)) return i;
        }
        return 0;
    }
}
