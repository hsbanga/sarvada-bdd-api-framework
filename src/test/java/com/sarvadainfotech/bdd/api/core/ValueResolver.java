package com.sarvadainfotech.bdd.api.core;

import com.sarvadainfotech.bdd.api.config.Config;
import com.sarvadainfotech.bdd.api.context.RequestContext;
import com.sarvadainfotech.bdd.api.util.RandomData;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the words used in feature files into concrete request values.
 *
 * <p>Two mechanisms:
 * <ul>
 *   <li><b>Conditions</b> for a named test-data item: {@code CORRECT}, {@code INCORRECT}, {@code INVALID},
 *       {@code NULL}, {@code EXPIRED}. The correct value comes from {@code data.<NAME>} in configuration;
 *       the others derive a deliberately wrong variant so negative tests need no extra data.</li>
 *   <li><b>Placeholders</b> inside any literal: {@code ${saved:key}} (value captured from an earlier
 *       response), {@code ${data:NAME}} (configuration), {@code ${random:email|uuid|int|string}}.</li>
 * </ul>
 */
public final class ValueResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(saved|data|random):([^}]+)}");

    private final RequestContext ctx;
    private final Config cfg = Config.get();

    public ValueResolver(RequestContext ctx) {
        this.ctx = ctx;
    }

    public String forCondition(String condition, String dataName) {
        String correct = cfg.data(dataName).or(() -> ctx.saved(dataName)).orElse(null);
        return switch (condition.toUpperCase(Locale.ROOT)) {
            case "CORRECT", "VALID" -> require(correct, dataName);
            case "INCORRECT" -> mutate(require(correct, dataName));
            case "INVALID" -> RandomData.alphanumeric(8);
            case "NULL", "EMPTY", "BLANK" -> "";
            case "EXPIRED" -> require(cfg.data("EXPIRED_" + dataName).orElse(null), "EXPIRED_" + dataName);
            default -> throw new IllegalArgumentException("Unknown condition '" + condition
                    + "'. Use CORRECT, INCORRECT, INVALID, NULL or EXPIRED.");
        };
    }

    /** Replaces every {@code ${...}} placeholder in the given text. */
    public String expand(String text) {
        if (text == null || !text.contains("${")) {
            return text;
        }
        Matcher m = PLACEHOLDER.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String kind = m.group(1);
            String key = m.group(2).trim();
            String value = switch (kind) {
                case "saved" -> ctx.saved(key).orElseThrow(() ->
                        new IllegalStateException("Nothing saved under '" + key + "' in this scenario"));
                case "data" -> cfg.data(key).orElseThrow(() ->
                        new IllegalStateException("No test data 'data." + key + "' for env " + cfg.env()));
                case "random" -> random(key);
                default -> m.group(0);
            };
            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String random(String type) {
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "email" -> RandomData.email();
            case "uuid" -> RandomData.uuid();
            case "int", "number" -> String.valueOf(RandomData.intBetween(1, 1_000_000));
            case "name" -> RandomData.firstName();
            default -> RandomData.alphanumeric(10);
        };
    }

    private static String require(String value, String name) {
        if (value == null) {
            throw new IllegalStateException("Test data 'data." + name + "' is not defined for this environment. "
                    + "Add it to config/<env>.properties or export DATA_" + name + ".");
        }
        return value;
    }

    /** Keeps the shape of the value but guarantees it differs from the original. */
    private static String mutate(String value) {
        if (value.isEmpty()) {
            return "x";
        }
        char last = value.charAt(value.length() - 1);
        char replacement = Character.isDigit(last) ? (last == '9' ? '0' : (char) (last + 1))
                : (last == 'z' || last == 'Z') ? 'a' : (char) (last + 1);
        return value.substring(0, value.length() - 1) + replacement;
    }
}
