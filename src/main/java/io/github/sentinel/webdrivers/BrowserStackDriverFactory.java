package io.github.sentinel.webdrivers;

import java.net.URL;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariOptions;

import io.github.sentinel.configurations.Configuration;
import io.github.sentinel.exceptions.MalformedURLException;

/**
 * Creates drivers for BrowserStack cloud testing.
 * Configure via sentinel.yml: set browserStackUserName and browserStackAccessKey.
 * Optional: browserStackOS, browserStackOSVersion, browserStackBuild, browserStackProject.
 */
public class BrowserStackDriverFactory {

    private static final Logger log = LogManager.getLogger(BrowserStackDriverFactory.class);
    private static final String HUB_URL = "https://hub.browserstack.com/wd/hub";

    private BrowserStackDriverFactory() {
    }

    protected static WebDriver createBrowserStackDriver() {
        URL browserStackUrl;
        try {
            browserStackUrl = new URL(HUB_URL);
        } catch (java.net.MalformedURLException e) {
            throw new MalformedURLException(e);
        }

        String browser = Configuration.browser();
        var browserOptions = buildOptionsForBrowser(browser);

        HashMap<String, Object> bstackOptions = new HashMap<>();
        bstackOptions.put("userName", Configuration.toString("browserStackUserName"));
        bstackOptions.put("accessKey", Configuration.toString("browserStackAccessKey"));

        String os = Configuration.toString("browserStackOS");
        if (os != null) bstackOptions.put("os", os);

        String osVersion = Configuration.toString("browserStackOSVersion");
        if (osVersion != null) bstackOptions.put("osVersion", osVersion);

        String browserVersion = Configuration.toString("browserVersion");
        bstackOptions.put("browserVersion", browserVersion != null ? browserVersion : "latest");

        String build = Configuration.toString("browserStackBuild");
        if (build != null) bstackOptions.put("buildName", build);

        String project = Configuration.toString("browserStackProject");
        if (project != null) bstackOptions.put("projectName", project);

        String testName = buildTestName();
        bstackOptions.put("sessionName", testName);

        browserOptions.setCapability("bstack:options", bstackOptions);
        log.debug("Starting BrowserStack session: {}", testName);

        return new RemoteWebDriver(browserStackUrl, browserOptions);
    }

    private static String buildTestName() {
        String name = Configuration.toString("name");
        String user = System.getProperty("user.name");
        StringBuilder sb = new StringBuilder(name != null ? name : "Sentinel Test");
        if (user != null) sb.append(" [").append(user).append("]");
        return sb.toString();
    }

    private static AbstractDriverOptions<?> buildOptionsForBrowser(String browser) {
        return switch (browser.toLowerCase()) {
            case "chrome" -> new ChromeOptions();
            case "firefox" -> new FirefoxOptions();
            case "edge" -> new EdgeOptions();
            case "safari" -> new SafariOptions();
            default -> {
                var opts = new ChromeOptions();
                opts.setCapability("browserName", browser);
                yield opts;
            }
        };
    }
}
