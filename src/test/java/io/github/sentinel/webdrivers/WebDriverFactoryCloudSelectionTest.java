package io.github.sentinel.webdrivers;

import org.junit.After;
import org.junit.Test;

import io.github.sentinel.configurations.Configuration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WebDriverFactoryCloudSelectionTest {

    @After
    public void tearDownAfterEachTest() {
        clearProperty("saucelabsUserName");
        clearProperty("saucelabsAccessKey");
    }

    @Test
    public void ignorePlaceholderSauceLabsCredentials() {
        System.setProperty("saucelabsUserName", "username");
        System.setProperty("saucelabsAccessKey", "apikey");

        assertFalse(WebDriverFactory.hasConfiguredCloudCredentials(
                "Sauce Labs", "saucelabsUserName", "saucelabsAccessKey", "username", "apikey"));
    }

    @Test
    public void ignoreIncompleteSauceLabsCredentials() {
        System.setProperty("saucelabsUserName", "real-user");

        assertFalse(WebDriverFactory.hasConfiguredCloudCredentials(
                "Sauce Labs", "saucelabsUserName", "saucelabsAccessKey", "username", "apikey"));
    }

    @Test
    public void acceptCompleteSauceLabsCredentials() {
        System.setProperty("saucelabsUserName", "real-user");
        System.setProperty("saucelabsAccessKey", "real-access-key");

        assertTrue(WebDriverFactory.hasConfiguredCloudCredentials(
                "Sauce Labs", "saucelabsUserName", "saucelabsAccessKey", "username", "apikey"));
    }

    private void clearProperty(String property) {
        Configuration.clear(property);
        System.clearProperty(property);
    }
}

