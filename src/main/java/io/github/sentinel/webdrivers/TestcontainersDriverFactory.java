package io.github.sentinel.webdrivers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;

import io.github.sentinel.configurations.Configuration;

/**
 * Creates a WebDriver backed by a Testcontainers-managed Selenium container.
 * Eliminates the need for WebDriverManager to download browser drivers in CI.
 * Requires testcontainers:selenium on the classpath (optional Maven dependency).
 * Enable by setting "containerized: true" in sentinel.yml.
 */
public class TestcontainersDriverFactory {

    private static final Logger log = LogManager.getLogger(TestcontainersDriverFactory.class);
    private static final ThreadLocal<BrowserWebDriverContainer<?>> containerHolder = new ThreadLocal<>();

    private TestcontainersDriverFactory() {
    }

    protected static WebDriver createContainerDriver() {
        String browser = Configuration.browser();
        BrowserWebDriverContainer<?> container = buildContainer(browser);
        container.start();
        containerHolder.set(container);
        log.info("Testcontainers {} browser started.", browser);
        return container.getWebDriver();
    }

    /**
     * Stops the Testcontainers browser container for the current thread.
     * Called automatically when the driver is quit via WebDriverFactory.
     */
    protected static void stopContainer() {
        BrowserWebDriverContainer<?> container = containerHolder.get();
        if (container != null && container.isRunning()) {
            container.stop();
            log.info("Testcontainers browser container stopped.");
        }
        containerHolder.remove();
    }

    private static BrowserWebDriverContainer<?> buildContainer(String browser) {
        return switch (browser.toLowerCase()) {
            case "firefox" -> new BrowserWebDriverContainer<>().withCapabilities(new FirefoxOptions());
            default -> new BrowserWebDriverContainer<>().withCapabilities(new ChromeOptions());
        };
    }
}
