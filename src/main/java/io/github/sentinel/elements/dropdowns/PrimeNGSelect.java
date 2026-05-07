package io.github.sentinel.elements.dropdowns;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import io.github.sentinel.configurations.Time;

/**
 * Implementation of a PrimeNG Select (p-select) component, introduced in PrimeNG v17
 * as the replacement for the deprecated p-dropdown component.
 * @see <a href="https://primeng.org/select">PrimeNG Select</a>
 * <p>
 * <b>Page Object Examples:</b>
 * <ul>
 * <li>city_dropdown:
 *   elementType: PrimeNGSelect
 *   xpath: "(//p-select)[1]"</li>
 * </ul>
 */
public class PrimeNGSelect extends JSDropdownElement {
    private static final Logger log = LogManager.getLogger(PrimeNGSelect.class.getName());

    private static final double SELECTWAITTIME = 0.1;

    public PrimeNGSelect(String elementName, Map<String, String> selectors) {
        super(elementName, selectors);
    }

    @Override
    protected WebElement getOption(String selectionText) {
        Time.wait(SELECTWAITTIME);
        String xPath = "//li[@aria-label=\"" + selectionText + "\"]";
        log.trace("Selecting option '{}' using xpath {}", selectionText, xPath);
        this.click();
        return this.element(By.xpath(xPath));
    }

    @Override
    protected WebElement getOption(int index) {
        Time.wait(SELECTWAITTIME);
        // aria-posinset is explicitly set on every option li by PrimeNG regardless of theme/class names
        String xPath = "//li[@aria-posinset='" + index + "']";
        log.trace("Selecting option at index {} using xpath {}", index, xPath);
        this.click();
        return this.element(By.xpath(xPath));
    }

    @Override
    public String getText(int index) {
        return getOption(index).getAttribute("aria-label");
    }

    @Override
    public String getText() {
        return this.element(By.xpath(".//span[contains(@class,'p-select-label')]")).getText();
    }
}
