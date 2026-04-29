package io.github.sentinel.webdrivers;
import io.github.sentinel.system.FileManager;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import io.appium.java_client.windows.options.WindowsOptions;
import io.github.sentinel.configurations.Configuration;
import io.appium.java_client.windows.WindowsDriver;

/**
 * @author Sentinel Framework
 *
 *         Creates WinAppDriver.
 */
public class WindowsDriverFactory {
	private static final Logger log = LogManager.getLogger(WindowsDriverFactory.class);
	private static Integer numberOfDriversRunning = 0;
	private static final String STDOUT = "logs/WinAppDriver.log";
	private static final String STDERR = "logs/WinAppDriverError.log";
	private static AppiumDriverLocalService appiumService;

	/**
	 * Exists to defeat instantiation.
	 */
	private WindowsDriverFactory() {}

	protected static void startAppiumService() {
		AppiumServiceBuilder builder = new AppiumServiceBuilder();
		builder.withIPAddress("127.0.0.1").usingPort(4723);
		appiumService = AppiumDriverLocalService.buildService(builder);
		appiumService.isRunning();
	}

	/**
	 * Creates a new Appium service and starts a new instance of WinAppDriver.exe associated to that service.
	 * <p>
	 * Returns a newly created WindowsDriver as a WebDriver, based on the currently
	 * active page object and environment.
	 * <p>
	 * Note: This method cannot tell if WinAppDriver.exe has already been started externally
	 * and will create a port conflict if it is already running.
	 *
	 * @return WebDriver returns a WindowsDriver&lt;WebElement&gt;
	 */
	protected static WebDriver createWindowsDriver() {
		if (numberOfDriversRunning == 0)
			startAppiumService();

		String executable = FileManager.winSpecialFolderConverter(Configuration.executable());

		var capabilities = new WindowsOptions();
		capabilities.setApp(executable);

		WindowsDriver driver = null;
		Exception driverCreationException = null;
		try {
			driver = new WindowsDriver(appiumService, capabilities);
		}
		catch (Exception e) {
			driverCreationException = e;
			log.error("{} Driver creation failed for: {}\n{}", e.getCause(), executable, e.getMessage());
		}

		if (driver == null) {
			String message = String.format(
				"WindowsDriver could not be created for executable '%s'. " +
				"Ensure Appium is running and the application path is correct.",
				executable);
			throw new IllegalStateException(message, driverCreationException);
		}

		log.info("Driver created: {}\nLog Location:       {}\nError Log Location: {}", driver, STDOUT, STDERR);
		numberOfDriversRunning += 1;
		return driver;
	}

	/**
	 * Quits the WindowsDriver process passed to it. If this is the last remaining
	 * WinAppDriver running, the WinAppDriver.exe executable is also stopped.
	 *
	 * @param driver WindowsDriver&lt;WebElement&gt; the WindowsDriver to quit
	 */
	protected static void quit(WindowsDriver driver) {
		driver.quit();
		numberOfDriversRunning -= 1;
		if (numberOfDriversRunning <= 0) {
			numberOfDriversRunning = 0;
			appiumService.stop();
		}
	}
}