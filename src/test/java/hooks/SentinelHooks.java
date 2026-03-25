package hooks;

import com.dougnoel.sentinel.configurations.Configuration;
import com.dougnoel.sentinel.enums.YAMLObjectType;
import com.dougnoel.sentinel.system.SentinelScreenRecorder;
import com.dougnoel.sentinel.system.TestManager;
import com.dougnoel.sentinel.webdrivers.Driver;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

public class SentinelHooks {

    private static final Logger log = LogManager.getLogger(SentinelHooks.class);

    @Before
    public void setUp() {
        if (Configuration.toBoolean("recordTests")) {
            try {
                SentinelScreenRecorder.startRecording();
            } catch (Exception e) {
                log.warn("Screen recording could not be started: {}", e.getMessage());
            }
        }
    }

    @After
    public void tearDown(Scenario scenario) {
        //Take screenshot and attach to report if scenario has failed
        if (scenario.isFailed() && TestManager.getActiveTestObject().getType() != YAMLObjectType.API) {
            final byte[] screenshot = ((TakesScreenshot) Driver.getWebDriver()).getScreenshotAs(OutputType.BYTES);
            scenario.attach(screenshot, "image/png", scenario.getName());
        }

        String totalWaitTime = Configuration.toString("totalWaitTime");
        if (totalWaitTime != null) {
            log.warn("This test took {} total seconds longer due to explicit waits. Sentinel handles dynamic waits. If you have a reason for adding explicit waits, you should probably be logging a bug ticket to get the framework fixed at: https://github.com/dougnoel/sentinel/issues", totalWaitTime);
        }

        if (Configuration.toBoolean("recordTests")) {
            try {
                SentinelScreenRecorder.stopRecording();
            } catch (Exception e) {
                log.warn("Screen recording could not be stopped: {}", e.getMessage());
            }
        }

        if (!Configuration.toBoolean("leaveBrowserOpen")) {
            Driver.quitAllDrivers();
        }
    }
}
