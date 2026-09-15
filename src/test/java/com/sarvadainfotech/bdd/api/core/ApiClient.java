package com.sarvadainfotech.bdd.api.core;

import com.sarvadainfotech.bdd.api.config.Config;
import com.sarvadainfotech.bdd.api.context.RequestContext;
import com.sarvadainfotech.bdd.api.context.RequestContext.Place;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Builds a Rest Assured request from the scenario's {@link RequestContext}, sends it, and records the
 * response. Handles base-URL lookup by API alias, JSON defaults, request/response logging into the
 * test log, and a bounded retry on HTTP 429 that honours {@code Retry-After}.
 */
public class ApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(ApiClient.class);

    private final RequestContext ctx;
    private final Config cfg = Config.get();

    public ApiClient(RequestContext ctx) {
        this.ctx = ctx;
    }

    public Response send(String method, String endpointNameOrPath) {
        String path = Endpoints.resolve(endpointNameOrPath);
        String verb = method.toUpperCase(Locale.ROOT);
        int attempts = Math.max(1, cfg.retryOn429MaxAttempts());
        Response response = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            ByteArrayOutputStream wire = new ByteArrayOutputStream();
            RequestSpecification spec = build(new PrintStream(wire, true, StandardCharsets.UTF_8));
            response = execute(spec, verb, path);
            if (cfg.logRequests()) {
                LOG.info("{} {} -> {} in {} ms\n{}", verb, path, response.statusCode(), response.time(),
                        mask(wire.toString(StandardCharsets.UTF_8)));
            }
            if (response.statusCode() != 429 || attempt == attempts) {
                break;
            }
            long waitMs = retryAfterMillis(response);
            LOG.warn("HTTP 429 from {} {}; retrying in {} ms (attempt {}/{})", verb, path, waitMs, attempt, attempts);
            sleep(waitMs);
        }
        ctx.recordResponse(endpointNameOrPath, response);
        ctx.resetRequest();
        return response;
    }

    private static final java.util.regex.Pattern SECRET_HEADER = java.util.regex.Pattern.compile(
            "(?im)^(\\s*)([^=\\r\\n]*(authorization|api[-_]?key|token|secret|password|cookie)[^=\\r\\n]*)=.*$");

    /** Hides credential-bearing header values before they reach the log file. */
    static String mask(String wire) {
        return SECRET_HEADER.matcher(wire).replaceAll("$1$2=*****");
    }

    private RequestSpecification build(PrintStream wireLog) {
        RestAssuredConfig raConfig = RestAssured.config().httpClient(HttpClientConfig.httpClientConfig()
                .setParam("http.connection.timeout", cfg.requestTimeoutSeconds() * 1000)
                .setParam("http.socket.timeout", cfg.requestTimeoutSeconds() * 1000));

        RequestSpecBuilder b = new RequestSpecBuilder()
                .setConfig(raConfig)
                .setBaseUri(cfg.baseUrl(ctx.apiAlias()))
                .setRelaxedHTTPSValidation()
                .addQueryParams(ctx.params(Place.QUERY))
                .addHeaders(ctx.params(Place.HEADER))
                .addPathParams(ctx.params(Place.PATH))
                .addCookies(ctx.params(Place.COOKIE))
                .addFilter(new RequestLoggingFilter(LogDetail.ALL, true, wireLog))
                .addFilter(new ResponseLoggingFilter(LogDetail.ALL, true, wireLog));

        if (!ctx.params(Place.FORM).isEmpty()) {
            b.setContentType(ContentType.URLENC).addFormParams(ctx.params(Place.FORM));
        } else if (ctx.body() != null) {
            if (!ctx.params(Place.HEADER).containsKey("Content-Type")) {
                b.setContentType(ContentType.JSON);
            }
            b.setBody(ctx.body());
        }
        return b.build();
    }

    private static Response execute(RequestSpecification spec, String verb, String path) {
        RequestSpecification given = RestAssured.given().spec(spec);
        return switch (verb) {
            case "GET" -> given.get(path);
            case "POST" -> given.post(path);
            case "PUT" -> given.put(path);
            case "PATCH" -> given.patch(path);
            case "DELETE" -> given.delete(path);
            case "HEAD" -> given.head(path);
            case "OPTIONS" -> given.options(path);
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + verb);
        };
    }

    private static long retryAfterMillis(Response response) {
        String header = response.getHeader("Retry-After");
        if (header != null) {
            try {
                return Long.parseLong(header.trim()) * 1000L;
            } catch (NumberFormatException ignored) {
                // HTTP-date form; fall through to default
            }
        }
        return 2000L;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
