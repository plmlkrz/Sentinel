package tests;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasspathResource("features")
@ConfigurationParameter(key = "cucumber.glue", value = "io.github.sentinel.steps,steps,hooks")
@ConfigurationParameter(key = "cucumber.plugin", value = "json:target/cucumber.json,com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:")
@ConfigurationParameter(key = "cucumber.ansi-colors.disabled", value = "true")
public class SentinelTests {
}
