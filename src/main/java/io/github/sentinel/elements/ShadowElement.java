package io.github.sentinel.elements;

import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import io.github.sentinel.configurations.Time;
import io.github.sentinel.enums.SelectorType;
import io.github.sentinel.strings.SentinelStringUtils;
import io.github.sentinel.webdrivers.Driver;

/**
 * Element implementation that pierces Shadow DOM boundaries.
 * Define in YAML with elementType: ShadowElement, a standard CSS selector for the
 * shadow host element, and a shadow_css selector for the inner element:
 * <pre>
 * my_button:
 *   elementType: ShadowElement
 *   css: "my-custom-component"
 *   shadow_css: "button.submit"
 * </pre>
 * Uses Selenium 4's native getShadowRoot() to traverse shadow roots.
 */
public class ShadowElement extends Element {

    public ShadowElement(String elementName, Map<String, String> selectors) {
        super("ShadowElement", elementName, selectors);
    }

    @Override
    protected WebElement element() {
        String hostCss = selectors.get(SelectorType.CSS);
        String shadowCss = selectors.get(SelectorType.SHADOW_CSS);

        if (hostCss == null || shadowCss == null) {
            return super.element();
        }

        long searchTimeMs = Time.out().getSeconds() * 1000;
        long startTime = System.currentTimeMillis();

        while ((System.currentTimeMillis() - startTime) < searchTimeMs) {
            try {
                WebElement host = Driver.getWebDriver().findElement(By.cssSelector(hostCss));
                if (host != null) {
                    SearchContext shadowRoot = host.getShadowRoot();
                    WebElement inner = shadowRoot.findElement(By.cssSelector(shadowCss));
                    if (inner != null) return inner;
                }
            } catch (Exception ignored) {
                // retry until timeout
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        String errorMessage = SentinelStringUtils.format(
                "ShadowElement \"{}\" not found. Host CSS: {}, Shadow CSS: {}",
                getName(), hostCss, shadowCss);
        throw new NoSuchElementException(errorMessage);
    }
}
