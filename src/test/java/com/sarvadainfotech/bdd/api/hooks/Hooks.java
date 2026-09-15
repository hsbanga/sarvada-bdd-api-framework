package com.sarvadainfotech.bdd.api.hooks;

import com.sarvadainfotech.bdd.api.context.RequestContext;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Scenario lifecycle logging plus attaching the last response to the report when a scenario fails. */
public class Hooks {

    private static final Logger LOG = LoggerFactory.getLogger(Hooks.class);

    private final RequestContext ctx;

    public Hooks(RequestContext ctx) {
        this.ctx = ctx;
    }

    @Before(order = 0)
    public void startScenario(Scenario scenario) {
        LOG.info("==== START  {}  [{}]", scenario.getName(), String.join(" ", scenario.getSourceTagNames()));
    }

    @After(order = 0)
    public void endScenario(Scenario scenario) {
        if (scenario.isFailed()) {
            try {
                Response r = ctx.lastResponse();
                String body = r.asPrettyString();
                scenario.attach(("HTTP " + r.statusCode() + " (" + r.time() + " ms)\n\n" + body).getBytes(),
                        "text/plain", "last response");
            } catch (IllegalStateException noResponse) {
                scenario.log("No HTTP response was received before the failure.");
            }
            if (!ctx.allSaved().isEmpty()) {
                scenario.attach(ctx.allSaved().toString().getBytes(), "text/plain", "saved values");
            }
        }
        LOG.info("==== END    {}  status={}", scenario.getName(), scenario.getStatus());
    }
}
