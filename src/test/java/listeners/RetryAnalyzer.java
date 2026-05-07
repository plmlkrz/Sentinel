package listeners;

import io.github.sentinel.configurations.Configuration;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * TestNG retry analyzer that re-runs failed tests up to a configurable limit.
 * Set "testRetryCount" in sentinel.yml to control the maximum number of retries (default: 1).
 * Wired globally via RetryTransformer — no per-test annotation required.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private int attempt = 0;

    @Override
    public boolean retry(ITestResult result) {
        int maxRetries = getMaxRetries();
        if (attempt < maxRetries) {
            attempt++;
            return true;
        }
        return false;
    }

    private static int getMaxRetries() {
        String configured = Configuration.toString("testRetryCount");
        if (configured == null) return 1;
        try {
            return Integer.parseInt(configured);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
