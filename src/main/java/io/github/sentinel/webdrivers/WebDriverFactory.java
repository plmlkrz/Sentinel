package io.github.sentinel.webdrivers;

import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.safari.SafariDriver;

import io.github.sentinel.configurations.Configuration;
import io.github.sentinel.strings.SentinelStringUtils;
import io.github.sentinel.system.DownloadManager;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * This object factory is used to keep up with driver versions for all browsers.
 * For a list of supported browsers and operating systems, see the readme.
 */
public class WebDriverFactory {
    private static final Logger log = LogManager.getLogger(WebDriverFactory.class);
    private static final ThreadLocal<WebDriver> driverHolder = new ThreadLocal<>();
    private static final String EXAMPLE_CHROME_BINARY = "my/path/here/executableName.exe";

    private WebDriverFactory() {
        // Exists only to defeat instantiation.
    }

    /**
     * Creates and returns a useable WebDriver.
     * We use this factory method to handle keeping up with driver versions for all
     * browsers. The browser can be set in the config file or a system
     * variable. See the README for more information.
     *
     * @return WebDriver An initialized <a href="https://www.seleniumhq.org/">Selenium
     * WebDriver</a> object for the specified browser and operating system
     * combination.
     */
    protected static WebDriver instantiateWebDriver() {
        WebDriver driver;

        // Testcontainers: containerized browser in CI
        if (Configuration.toBoolean("containerized")) {
            driver = TestcontainersDriverFactory.createContainerDriver();
            driverHolder.set(driver);
            return driver;
        }

        // SauceLabs cloud
        if (hasConfiguredCloudCredentials("Sauce Labs", "saucelabsUserName", "saucelabsAccessKey", "username", "apikey")) {
            driver = SauceLabsDriverFactory.createSaucelabsDriver();
            driverHolder.set(driver);
            return driver;
        }

        // BrowserStack cloud
        if (hasConfiguredCloudCredentials("BrowserStack", "browserStackUserName", "browserStackAccessKey", "username", "accesskey")) {
            driver = BrowserStackDriverFactory.createBrowserStackDriver();
            driverHolder.set(driver);
            return driver;
        }

        // LambdaTest cloud
        if (hasConfiguredCloudCredentials("LambdaTest", "lambdaTestUserName", "lambdaTestAccessKey", "username", "accesskey")) {
            driver = LambdaTestDriverFactory.createLambdaTestDriver();
            driverHolder.set(driver);
            return driver;
        }

        var browser = Configuration.browser();

        // Selenium Grid
        var gridUrl = Configuration.toString("gridUrl");
        if (StringUtils.isNotBlank(gridUrl)) {
            driver = GridWebDriverFactory.createGridDriver(browser, gridUrl);
            driverHolder.set(driver);
            return driver;
        }

        // Local browser
        switch (browser) {
            case "chrome":
                driver = createChromeDriver();
                break;
            case "edge":
                WebDriverManager.edgedriver().setup();
                driver = new EdgeDriver();
                break;
            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                driver = new FirefoxDriver();
                break;
            case "safari":
                driver = new SafariDriver();
                break;
            default:
                throw new WebDriverException(SentinelStringUtils.format("Invalid browser type '{}' passed to WebDriverFactory. Could not resolve the reference. Check your spelling. Refer to the Javadoc for valid options.", browser));
        }

        driverHolder.set(driver);
        return driver;
    }

    /**
     * Returns the WebDriver instance. This will silently instantiate the WebDriver if that has not been done yet.
     *
     * @return WebDriver the created Selenium WebDriver
     */
    public static WebDriver getWebDriver() {
        if (driverHolder.get() == null) {
            instantiateWebDriver();
            log.info("Driver created: {}", driverHolder.get());
        }
        return driverHolder.get();
    }

    /**
     * Quits the driver and sets the driver instance back to null.
     */
    protected static void quit() {
        if (exists()) {
            driverHolder.get().quit();
            if (Configuration.toBoolean("containerized")) {
                TestcontainersDriverFactory.stopContainer();
            }
            driverHolder.remove();
        } else {
            log.info("Attempted to call quit on a driver that did not exist.");
        }
    }

    public static boolean exists() {
        return driverHolder.get() != null;
    }

    /**
     * Sets the download directory for chromedriver. Cannot be used with Saucelabs.
     *
     * @param options ChromeOptions object to set
     */
    private static ChromeOptions setChromeDownloadDirectory(ChromeOptions options) {
        HashMap<String, Object> chromePrefs = new HashMap<>();
        chromePrefs.put("download.prompt_for_download", false);
        chromePrefs.put("download.default_directory", DownloadManager.getDownloadDirectory());
        return options.setExperimentalOption("prefs", chromePrefs);
    }

    /**
     * Creates a ChromeDriver. Makes it headless if the -Dheadless flag is set.
     * Can pass additional arguments with the -DchromeOptions flag, such as -DchromeOptions="start-maximized" to open all browser windows maximized.
     *
     * @return WebDriver ChromeDrvier
     */
    private static WebDriver createChromeDriver() {
        var chromeOptions = setChromeDownloadDirectory(new ChromeOptions());
        String commandlineOptions = Configuration.toString("chromeOptions");
        if (commandlineOptions != null)
            chromeOptions.addArguments(commandlineOptions);
        if (Configuration.toBoolean("headless")) {
            chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--disable-dev-shm-usage");
            chromeOptions.addArguments("--headless=new");
        }
        var binary = sanitizeChromeBinary(Configuration.toString("chromeBrowserBinary"));
        if (binary != null)
            chromeOptions.setBinary(binary);
        WebDriverManager.chromedriver().setup();
        return new ChromeDriver(chromeOptions);
    }

    static String sanitizeChromeBinary(String configuredBinary) {
        var binary = StringUtils.trimToNull(configuredBinary);
        if (EXAMPLE_CHROME_BINARY.equalsIgnoreCase(binary)) {
            log.warn("Chrome binary configuration ignored because the example placeholder path is configured. Remove the placeholder for standard local Chrome discovery or replace it with a valid Chrome executable path.");
            return null;
        }

        return binary;
    }

    static boolean hasConfiguredCloudCredentials(String providerName, String userNameProperty, String accessKeyProperty,
                                                 String placeholderUserName, String placeholderAccessKey) {
        var userName = StringUtils.trimToNull(Configuration.toString(userNameProperty));
        var accessKey = StringUtils.trimToNull(Configuration.toString(accessKeyProperty));

        if (userName == null && accessKey == null) {
            return false;
        }

        if (userName == null || accessKey == null) {
            log.warn("{} configuration ignored because both '{}' and '{}' must be set before remote execution can start.",
                    providerName, userNameProperty, accessKeyProperty);
            return false;
        }

        if (userName.equalsIgnoreCase(placeholderUserName) || accessKey.equalsIgnoreCase(placeholderAccessKey)) {
            log.warn("{} configuration ignored because placeholder credentials are configured. Remove the example values for local runs or replace them with valid credentials for remote execution.",
                    providerName);
            return false;
        }

        return true;
    }

}