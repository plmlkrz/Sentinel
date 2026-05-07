package io.github.sentinel.webdrivers;

import io.github.sentinel.configurations.Configuration;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class WebDriverFactoryCloudSelectionTest {

    @AfterMethod
    public void tearDownAfterEachTest() {
        clearProperty("saucelabsUserName");
        clearProperty("saucelabsAccessKey");
    }

    @Test
    public void ignorePlaceholderSauceLabsCredentials() {
        System.setProperty("saucelabsUserName", "username");
        System.setProperty("saucelabsAccessKey", "apikey");

        Assert.assertFalse(WebDriverFactory.hasConfiguredCloudCredentials(
                "Sauce Labs", "saucelabsUserName", "saucelabsAccessKey", "username", "apikey"));
    }

    @Test
    public void ignoreIncompleteSauceLabsCredentials() {
        System.setProperty("saucelabsUserName", "real-user");

        Assert.assertFalse(WebDriverFactory.hasConfiguredCloudCredentials(
                "Sauce Labs", "saucelabsUserName", "saucelabsAccessKey", "username", "apikey"));
    }

    @Test
    public void acceptCompleteSauceLabsCredentials() {
        System.setProperty("saucelabsUserName", "real-user");
        System.setProperty("saucelabsAccessKey", "real-access-key");

        Assert.assertTrue(WebDriverFactory.hasConfiguredCloudCredentials(
                "Sauce Labs", "saucelabsUserName", "saucelabsAccessKey", "username", "apikey"));
    }

    @Test
    public void ignorePlaceholderChromeBinary() {
        Assert.assertNull(WebDriverFactory.sanitizeChromeBinary("my/path/here/executableName.exe"));
    }

    @Test
    public void keepValidChromeBinary() {
        Assert.assertEquals(WebDriverFactory.sanitizeChromeBinary("C:/Program Files/Google/Chrome/Application/chrome.exe"),
                "C:/Program Files/Google/Chrome/Application/chrome.exe");
    }

    private void clearProperty(String property) {
        Configuration.clear(property);
        System.clearProperty(property);
    }
}



