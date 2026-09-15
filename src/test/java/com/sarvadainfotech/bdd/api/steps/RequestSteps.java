package com.sarvadainfotech.bdd.api.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sarvadainfotech.bdd.api.context.RequestContext;
import com.sarvadainfotech.bdd.api.context.RequestContext.Place;
import com.sarvadainfotech.bdd.api.core.ApiClient;
import com.sarvadainfotech.bdd.api.core.ValueResolver;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Reusable vocabulary for building and sending HTTP requests. No step here knows anything about a
 * particular API; endpoint names, base URLs and test data all come from configuration.
 */
public class RequestSteps {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final RequestContext ctx;
    private final ValueResolver values;
    private final ApiClient client;

    public RequestSteps(RequestContext ctx) {
        this.ctx = ctx;
        this.values = new ValueResolver(ctx);
        this.client = new ApiClient(ctx);
    }

    @Given("I use the {string} API")
    public void iUseTheApi(String alias) {
        ctx.useApi(alias);
    }

    /** e.g. {@code Given I add "CORRECT" "API_KEY" in "HEADER"}. */
    @Given("I add {string} {string} in {string}")
    public void iAddConditionValue(String condition, String dataName, String place) {
        ctx.set(place(place), dataName, values.forCondition(condition, dataName));
    }

    /** Same as above when the request parameter name differs from the data name:
     *  {@code Given I add "CORRECT" "USER_ID" in "PATH" as "id"}. */
    @Given("I add {string} {string} in {string} as {string}")
    public void iAddConditionValueAs(String condition, String dataName, String place, String paramName) {
        ctx.set(place(place), paramName, values.forCondition(condition, dataName));
    }

    /** e.g. {@code Given I add "userId" as "1" in "QUERY"}. Literal values may contain placeholders. */
    @Given("I add {string} as {string} in {string}")
    public void iAddLiteral(String name, String value, String place) {
        ctx.set(place(place), name, values.expand(value));
    }

    @Given("I add the following in {string}:")
    public void iAddTable(String place, Map<String, String> entries) {
        entries.forEach((k, v) -> ctx.set(place(place), k, values.expand(v)));
    }

    @Given("I set the request body:")
    public void iSetTheRequestBody(String docString) {
        ctx.setBody(values.expand(docString));
    }

    @Given("I set the request body from file {string}")
    public void iSetTheRequestBodyFromFile(String classpathFile) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(classpathFile)) {
            if (in == null) {
                throw new IllegalArgumentException("Body file not found on classpath: " + classpathFile);
            }
            ctx.setBody(values.expand(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + classpathFile, e);
        }
    }

    /** Builds a flat JSON object from a two-column table; numeric and boolean values are typed. */
    @Given("I set the request body to:")
    public void iSetTheRequestBodyTo(Map<String, String> fields) {
        Map<String, Object> body = new LinkedHashMap<>();
        fields.forEach((k, v) -> body.put(k, typed(values.expand(v))));
        try {
            ctx.setBody(JSON.writeValueAsString(body));
        } catch (IOException e) {
            throw new IllegalStateException("Could not serialise request body", e);
        }
    }

    @When("I send a {word} request to {string}")
    public void iSendARequestTo(String method, String endpoint) {
        client.send(method, endpoint);
    }

    // ---- helpers --------------------------------------------------------------------------------

    private static Place place(String place) {
        try {
            return Place.valueOf(place.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown place '" + place + "'. Use QUERY, HEADER, PATH, FORM or COOKIE.");
        }
    }

    private static Object typed(String v) {
        if (v == null) {
            return null;
        }
        if ("null".equalsIgnoreCase(v)) {
            return null;
        }
        if ("true".equalsIgnoreCase(v) || "false".equalsIgnoreCase(v)) {
            return Boolean.parseBoolean(v);
        }
        if (v.matches("-?\\d+")) {
            try {
                return Long.parseLong(v);
            } catch (NumberFormatException ignored) {
                return v;
            }
        }
        if (v.matches("-?\\d+\\.\\d+")) {
            return Double.parseDouble(v);
        }
        return v;
    }
}
