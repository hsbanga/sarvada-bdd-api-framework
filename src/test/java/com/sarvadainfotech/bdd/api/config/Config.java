package com.sarvadainfotech.bdd.api.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

/**
 * Layered, read-only configuration.
 *
 * <p>Resolution order for a key (first hit wins):
 * <ol>
 *   <li>JVM system property: {@code -Dkey=value}</li>
 *   <li>Environment variable: upper snake case, e.g. {@code api.default.base.url} becomes {@code API_DEFAULT_BASE_URL}</li>
 *   <li>{@code config/local.properties} (git-ignored, developer overrides)</li>
 *   <li>{@code config/<env>.properties} where env is {@code -Denv} (default {@code dev})</li>
 *   <li>{@code config/global.properties}</li>
 * </ol>
 * Secrets such as API keys must never be committed; supply them through the environment.
 */
public final class Config {

    private static final Config INSTANCE = new Config();

    private final Properties props = new Properties();
    private final String env;

    private Config() {
        env = System.getProperty("env", System.getenv().getOrDefault("ENV", "dev")).toLowerCase(Locale.ROOT);
        load("config/global.properties", true);
        load("config/" + env + ".properties", true);
        load("config/local.properties", false);
    }

    public static Config get() {
        return INSTANCE;
    }

    private void load(String classpathResource, boolean required) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) {
                if (required) {
                    throw new IllegalStateException("Missing required config file on classpath: " + classpathResource);
                }
                return;
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + classpathResource, e);
        }
    }

    public String env() {
        return env;
    }

    public Optional<String> find(String key) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) {
            return Optional.of(sys.trim());
        }
        String envVar = System.getenv(toEnvName(key));
        if (envVar != null && !envVar.isBlank()) {
            return Optional.of(envVar.trim());
        }
        String file = props.getProperty(key);
        return (file == null || file.isBlank()) ? Optional.empty() : Optional.of(file.trim());
    }

    public String getString(String key) {
        return find(key).orElseThrow(() ->
                new IllegalStateException("Configuration key '" + key + "' is not set. "
                        + "Set it with -D" + key + "=..., env var " + toEnvName(key)
                        + ", or in config/*.properties"));
    }

    public String getString(String key, String defaultValue) {
        return find(key).orElse(defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        return find(key).map(Integer::parseInt).orElse(defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return find(key).map(Boolean::parseBoolean).orElse(defaultValue);
    }

    static String toEnvName(String key) {
        return key.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
    }

    // Convenience accessors for the keys the framework itself uses.

    /** Base URL for a named API, e.g. {@code api.default.base.url} or {@code api.payments.base.url}. */
    public String baseUrl(String apiAlias) {
        return getString("api." + apiAlias.toLowerCase(Locale.ROOT) + ".base.url");
    }

    /** Named test-data value, stored as {@code data.<NAME>} in the environment file or as env var {@code DATA_<NAME>}. */
    public Optional<String> data(String name) {
        return find("data." + name);
    }

    public int requestTimeoutSeconds() {
        return getInt("request.timeout.seconds", 30);
    }

    public int retryOn429MaxAttempts() {
        return getInt("retry.429.max.attempts", 3);
    }

    public boolean logRequests() {
        return getBoolean("log.requests", true);
    }
}
