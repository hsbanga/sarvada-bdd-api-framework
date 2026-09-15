package com.sarvadainfotech.bdd.api.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Registry of named endpoints loaded from {@code endpoints.properties}, e.g. {@code USERS=/users}.
 * Feature files refer to endpoints by name so a URL change is a one-line edit. A value that already
 * starts with {@code /} or {@code http} is treated as a literal path so ad-hoc calls still work.
 */
public final class Endpoints {

    private static final Properties REGISTRY = new Properties();

    static {
        try (InputStream in = Endpoints.class.getClassLoader().getResourceAsStream("endpoints.properties")) {
            if (in == null) {
                throw new IllegalStateException("endpoints.properties not found on the test classpath");
            }
            REGISTRY.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read endpoints.properties", e);
        }
    }

    private Endpoints() {
    }

    public static String resolve(String nameOrPath) {
        if (nameOrPath.startsWith("/") || nameOrPath.startsWith("http")) {
            return nameOrPath;
        }
        String path = REGISTRY.getProperty(nameOrPath);
        if (path == null) {
            throw new IllegalArgumentException("Unknown endpoint '" + nameOrPath
                    + "'. Add it to src/test/resources/endpoints.properties or use a literal path starting with /");
        }
        return path.trim();
    }
}
