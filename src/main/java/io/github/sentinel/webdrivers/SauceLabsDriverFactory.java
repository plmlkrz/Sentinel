package io.github.sentinel.webdrivers;

import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariOptions;

import io.github.sentinel.configurations.Configuration;
import io.github.sentinel.exceptions.MalformedURLException;
import io.github.sentinel.strings.SentinelStringUtils;

/**
 * Creates drivers for Saucelabs.
 */
public class SauceLabsDriverFactory {
	private static final Logger log = LogManager.getLogger(SauceLabsDriverFactory.class);

	private SauceLabsDriverFactory() {
		//exists to defeat instantiation
	}

	/**
	 * Creates a single threaded Saucelabs WebDriver.
	 * @return WebDriver a Saucelabs WebDriver
	 */
	protected static WebDriver createSaucelabsDriver() {
		URL sauceLabsUrl;
		try {
			sauceLabsUrl = new URL("https://ondemand.saucelabs.com:443/wd/hub");
		} catch (java.net.MalformedURLException e) {
			throw new MalformedURLException(e);
		}

		String browser = Configuration.browser();
		String operatingSystem = Configuration.operatingSystem();

		var browserOptions = buildOptionsForBrowser(browser);

		var sauceOptions = new MutableCapabilities();
		setSaucelabsProperty("platform", operatingSystem, sauceOptions);
		var browserVersion = Configuration.toString("browserVersion");
		if (browserVersion != null) {
			setSaucelabsProperty("version", browserVersion, sauceOptions);
		} else {
			setSaucelabsProperty("version", "latest", sauceOptions);
		}
		setSaucelabsProperty("username", Configuration.toString("saucelabsUserName"), sauceOptions);
		setSaucelabsProperty("accesskey", Configuration.toString("saucelabsAccessKey"), sauceOptions);

		setSaucelabsTestNameProperty(sauceOptions);
		setSaucelabsProperties(sauceOptions); //called at the end to allow overwriting things set above

		browserOptions.setCapability("sauce:options", sauceOptions);

		return new RemoteWebDriver(sauceLabsUrl, browserOptions);
	}

	/**
	 * Searches for a saucelabsConfigs value containing a comma delimited list of properties and values
	 * in the form -DsaucelabsConfigs="parent-tunnel=myTunnel, tunnelIdentifier=tunnelID"
	 *
	 * @param options MutableCapabilities the capabilities object that stores all the properties for creating a RemoteWebDriver
	 */
	private static void setSaucelabsProperties(MutableCapabilities options) {
		var properties = SentinelStringUtils.stripSurroundingQuotes(Configuration.toString("saucelabsConfigs"));
		if (properties != null) {
			for (String property : properties.split(",")) {
				String[] prop = property.split("=");
				setSaucelabsProperty(prop[0].strip(), prop[1].strip(), options);
			}
		}
	}

	/**
	 * Sets a single Saucelabs property.
	 *
	 * @param saucelabsPropertyName String the property name
	 * @param saucelabsProperty String the value to set
	 * @param options MutableCapabilities the capabilities object that stores all the properties for creating a RemoteWebDriver
	 */
	private static void setSaucelabsProperty(String saucelabsPropertyName, String saucelabsProperty, MutableCapabilities options) {
		options.setCapability(saucelabsPropertyName, saucelabsProperty);
		log.debug("Saucelabs property set: {}={}", saucelabsPropertyName, saucelabsProperty);
	}

	/**
	 * Pulls config values and sets a string to pass to Saucelabs for the job.
	 * @param options MutableCapabilities the capabilities object that stores all the properties for creating a RemoteWebDriver
	 */
	private static void setSaucelabsTestNameProperty(MutableCapabilities options) {
		var testName = "";
		var jobName = Configuration.toString("name");
		if (jobName != null) {
			testName += "Name: " + jobName;
		} else {
			testName += "Default Sentinel Test Name";
		}

		var userName = System.getProperty("user.name");
		if (userName != null) {
			testName += " User: " + userName;
		}

		options.setCapability("name", testName);
		log.debug("Saucelabs name set: {}", testName);
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
