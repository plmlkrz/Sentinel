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
 * Creates drivers for LambdaTest cloud testing.
 * Configure via sentinel.yml: set lambdaTestUserName and lambdaTestAccessKey.
 * Optional: lambdaTestPlatform, lambdaTestBuild, lambdaTestProject.
 */
public class LambdaTestDriverFactory {

    private static final Logger log = LogManager.getLogger(LambdaTestDriverFactory.class);

    private LambdaTestDriverFactory() {
    }

    protected static WebDriver createLambdaTestDriver() {
        String username = Configuration.toString("lambdaTestUserName");
        String accessKey = Configuration.toString("lambdaTestAccessKey");

        URL lambdaTestUrl;
        try {
            lambdaTestUrl = new URL("https://" + username + ":" + accessKey + "@hub.lambdatest.com/wd/hub");
        } catch (java.net.MalformedURLException e) {
            throw new MalformedURLException(e);
        }

        String browser = Configuration.browser();
        var browserOptions = buildOptionsForBrowser(browser);

        HashMap<String, Object> ltOptions = new HashMap<>();
        ltOptions.put("username", username);
        ltOptions.put("accessKey", accessKey);

        String platform = Configuration.toString("lambdaTestPlatform");
        if (platform != null) ltOptions.put("platformName", platform);

        String browserVersion = Configuration.toString("browserVersion");
        ltOptions.put("browserVersion", browserVersion != null ? browserVersion : "latest");

        String build = Configuration.toString("lambdaTestBuild");
        if (build != null) ltOptions.put("build", build);

        String project = Configuration.toString("lambdaTestProject");
        if (project != null) ltOptions.put("project", project);

        String testName = buildTestName();
        ltOptions.put("name", testName);
        ltOptions.put("w3c", true);

        browserOptions.setCapability("LT:Options", ltOptions);
        log.debug("Starting LambdaTest session: {}", testName);

        return new RemoteWebDriver(lambdaTestUrl, browserOptions);
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
