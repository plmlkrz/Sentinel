package io.github.sentinel.webdrivers;

import java.net.URL;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariOptions;
import io.github.sentinel.configurations.Configuration;
import io.github.sentinel.exceptions.MalformedURLException;

public class GridWebDriverFactory {

	private static final String BROWSERVERSION = "browserVersion";

	private GridWebDriverFactory() {
	}

	protected static WebDriver createGridDriver(String browser, String gridUrl) {
		URL url;
		try {
			url = new URL(gridUrl);
		} catch (java.net.MalformedURLException e) {
			throw new MalformedURLException(e);
		}

		var options = buildOptionsForBrowser(browser);
		var browserVersion = Configuration.toString(BROWSERVERSION);
		if (browserVersion != null) {
			options.setBrowserVersion(browserVersion);
		} else {
			options.setBrowserVersion(getDefaultBrowserVersion(url));
		}

		return new RemoteWebDriver(url, options);
	}

	private static String getDefaultBrowserVersion(URL url) {
		var options = new FirefoxOptions();
		var driver = new RemoteWebDriver(url, options);
		var cap = driver.getCapabilities();
		driver.quit();
		return cap.getBrowserVersion();
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
