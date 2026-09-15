package com.sarvadainfotech.bdd.api.steps;

import com.sarvadainfotech.bdd.api.context.RequestContext;
import com.sarvadainfotech.bdd.api.core.ValueResolver;
import io.cucumber.java.en.Then;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/** Reusable assertions over the most recent response. */
public class ResponseSteps {

    private final RequestContext ctx;
    private final ValueResolver values;

    public ResponseSteps(RequestContext ctx) {
        this.ctx = ctx;
        this.values = new ValueResolver(ctx);
    }

    private Response last() {
        return ctx.lastResponse();
    }

    @Then("the response status code should be {int}")
    public void statusCodeShouldBe(int expected) {
        assertThat(last().statusCode())
                .as("HTTP status. Body:%n%s", last().asPrettyString())
                .isEqualTo(expected);
    }

    @Then("the response time should be under {int} ms")
    public void responseTimeUnder(int millis) {
        assertThat(last().time()).as("response time (ms)").isLessThan(millis);
    }

    @Then("the response header {string} should contain {string}")
    public void headerShouldContain(String header, String expected) {
        assertThat(last().getHeader(header)).as("header " + header).containsIgnoringCase(expected);
    }

    @Then("the response field {string} should be {string}")
    public void fieldShouldBe(String jsonPath, String expected) {
        Object actual = last().jsonPath().get(jsonPath);
        assertThat(String.valueOf(actual)).as("field " + jsonPath).isEqualTo(values.expand(expected));
    }

    @Then("the response field {string} should contain {string}")
    public void fieldShouldContain(String jsonPath, String expected) {
        Object actual = last().jsonPath().get(jsonPath);
        assertThat(String.valueOf(actual)).as("field " + jsonPath).contains(values.expand(expected));
    }

    @Then("the response field {string} should exist")
    public void fieldShouldExist(String jsonPath) {
        assertThat((Object) last().jsonPath().get(jsonPath)).as("field " + jsonPath).isNotNull();
    }

    @Then("the response field {string} should not exist")
    public void fieldShouldNotExist(String jsonPath) {
        assertThat((Object) last().jsonPath().get(jsonPath)).as("field " + jsonPath).isNull();
    }

    @Then("the response field {string} should be a valid URL")
    public void fieldShouldBeValidUrl(String jsonPath) {
        Object raw = last().jsonPath().get(jsonPath);
        String value = String.valueOf(raw);
        assertThat(URI.create(value).getScheme()).as("URL scheme of " + value).isIn("http", "https");
    }

    @Then("the response body should be empty")
    public void bodyShouldBeEmpty() {
        String body = last().asString().trim();
        assertThat(body).as("body").isIn("", "{}", "[]");
    }

    @Then("the response should be a list of {int} items")
    public void rootListSize(int expected) {
        assertThat(last().jsonPath().getList("$")).as("root array").hasSize(expected);
    }

    @Then("the response field {string} should be a list of {int} items")
    public void listSize(String jsonPath, int expected) {
        assertThat(last().jsonPath().getList(jsonPath)).as("list " + jsonPath).hasSize(expected);
    }

    @Then("the response field {string} should be a non-empty list")
    public void listNotEmpty(String jsonPath) {
        assertThat(last().jsonPath().getList(jsonPath)).as("list " + jsonPath).isNotEmpty();
    }

    @Then("every item in {string} should have {string} equal to {string}")
    public void everyItemHasFieldEqual(String listPath, String field, String expected) {
        List<Map<String, Object>> items = last().jsonPath().getList(listPath);
        String want = values.expand(expected);
        assertThat(items).as("list " + listPath).isNotEmpty();
        assertThat(items).allSatisfy(item ->
                assertThat(String.valueOf(item.get(field))).as("item." + field).isEqualTo(want));
    }

    @Then("the response should match the schema {string}")
    public void shouldMatchSchema(String classpathSchema) {
        last().then().assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath(classpathSchema));
    }

    @Then("I save the response field {string} as {string}")
    public void saveField(String jsonPath, String key) {
        Object value = last().jsonPath().get(jsonPath);
        assertThat((Object) value).as("field " + jsonPath + " to save").isNotNull();
        ctx.save(key, String.valueOf(value));
    }

    @Then("the response field {string} should equal the saved {string}")
    public void fieldEqualsSaved(String jsonPath, String key) {
        String saved = ctx.saved(key).orElseThrow(() -> new IllegalStateException("Nothing saved as " + key));
        Object actual = last().jsonPath().get(jsonPath);
        assertThat(String.valueOf(actual)).as("field " + jsonPath).isEqualTo(saved);
    }

    @Then("the {string} response field {string} should equal the {string} response field {string}")
    public void crossResponseFieldsEqual(String aliasA, String pathA, String aliasB, String pathB) {
        Object a = ctx.response(aliasA).orElseThrow(() -> new IllegalStateException("No response for " + aliasA))
                .jsonPath().get(pathA);
        Object b = ctx.response(aliasB).orElseThrow(() -> new IllegalStateException("No response for " + aliasB))
                .jsonPath().get(pathB);
        assertThat(Objects.toString(a)).as(aliasA + "." + pathA + " vs " + aliasB + "." + pathB)
                .isEqualTo(Objects.toString(b));
    }
}
