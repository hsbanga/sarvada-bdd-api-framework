package com.sarvadainfotech.bdd.api.runner;

import com.sarvadainfotech.bdd.api.config.Config;
import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;

/**
 * TestNG entry point. Feature and glue locations live here; everything runtime-related
 * (tags, environment, thread count) is passed on the command line, see README.
 */
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"com.sarvadainfotech.bdd.api.steps", "com.sarvadainfotech.bdd.api.hooks"},
        plugin = {
                "pretty",
                "summary",
                "html:target/cucumber-reports/cucumber.html",
                "json:target/cucumber-reports/cucumber.json",
                "junit:target/cucumber-reports/cucumber-junit.xml",
                "timeline:target/cucumber-reports/timeline",
                "rerun:target/cucumber-reports/rerun.txt"
        },
        monochrome = true,
        snippets = CucumberOptions.SnippetType.CAMELCASE
)
public class TestRunner extends AbstractTestNGCucumberTests {

    private static final Logger LOG = LoggerFactory.getLogger(TestRunner.class);

    @BeforeClass(alwaysRun = true)
    public void logRunConfiguration(ITestContext context) {
        Config cfg = Config.get();
        LOG.info("Environment : {}", cfg.env());
        LOG.info("Default API : {}", cfg.baseUrl("default"));
        LOG.info("Threads     : {}", context.getCurrentXmlTest().getSuite().getDataProviderThreadCount());
        LOG.info("Tags        : {}", System.getProperty("cucumber.filter.tags", "(all)"));
    }

    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
