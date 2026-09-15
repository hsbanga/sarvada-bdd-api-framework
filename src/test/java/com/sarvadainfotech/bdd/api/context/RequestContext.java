package com.sarvadainfotech.bdd.api.context;

import io.restassured.response.Response;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Everything one scenario accumulates while it runs: the request being built, the responses received
 * and any values saved from them. PicoContainer creates a fresh instance per scenario and injects it
 * into every step class, so scenarios are fully isolated and parallel-safe.
 */
public class RequestContext {

    /** Where a parameter goes on the outgoing request. */
    public enum Place { QUERY, HEADER, PATH, FORM, COOKIE }

    private String apiAlias = "default";
    private final Map<Place, Map<String, String>> params = new HashMap<>();
    private Object body;
    private Response lastResponse;
    private final Map<String, Response> responsesByAlias = new LinkedHashMap<>();
    private final Map<String, String> saved = new HashMap<>();

    public RequestContext() {
        for (Place p : Place.values()) {
            params.put(p, new LinkedHashMap<>());
        }
    }

    // ---- request under construction ---------------------------------------------------------

    public String apiAlias() {
        return apiAlias;
    }

    public void useApi(String alias) {
        this.apiAlias = alias;
    }

    public Map<String, String> params(Place place) {
        return params.get(place);
    }

    public void set(Place place, String name, String value) {
        params.get(place).put(name, value);
    }

    public Object body() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }

    /** Clears parameters and body so the next request starts clean; keeps responses and saved values. */
    public void resetRequest() {
        params.values().forEach(Map::clear);
        body = null;
    }

    // ---- responses -----------------------------------------------------------------------------

    public Response lastResponse() {
        if (lastResponse == null) {
            throw new IllegalStateException("No request has been sent yet in this scenario.");
        }
        return lastResponse;
    }

    public void recordResponse(String alias, Response response) {
        this.lastResponse = response;
        responsesByAlias.put(alias, response);
    }

    public Optional<Response> response(String alias) {
        return Optional.ofNullable(responsesByAlias.get(alias));
    }

    // ---- saved values ---------------------------------------------------------------------------

    public void save(String key, String value) {
        saved.put(key, value);
    }

    public Optional<String> saved(String key) {
        return Optional.ofNullable(saved.get(key));
    }

    public Map<String, String> allSaved() {
        return Map.copyOf(saved);
    }
}
