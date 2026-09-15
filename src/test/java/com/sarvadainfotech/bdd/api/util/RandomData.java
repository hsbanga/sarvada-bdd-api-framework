package com.sarvadainfotech.bdd.api.util;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

/** Small, dependency-free generators for unique test data. */
public final class RandomData {

    private static final SecureRandom RND = new SecureRandom();
    private static final String ALNUM = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final List<String> FIRST_NAMES =
            List.of("Asha", "Rohan", "Meera", "Kabir", "Priya", "Arjun", "Nisha", "Dev", "Tara", "Vikram");
    private static final List<String> LAST_NAMES =
            List.of("Sharma", "Verma", "Singh", "Iyer", "Patel", "Mehta", "Rao", "Nair", "Gupta", "Das");

    private RandomData() {
    }

    public static String alphanumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALNUM.charAt(RND.nextInt(ALNUM.length())));
        }
        return sb.toString();
    }

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    public static int intBetween(int minInclusive, int maxInclusive) {
        return minInclusive + RND.nextInt(maxInclusive - minInclusive + 1);
    }

    public static String email() {
        return "qa." + alphanumeric(8) + "@example.com";
    }

    public static String firstName() {
        return FIRST_NAMES.get(RND.nextInt(FIRST_NAMES.size()));
    }

    public static String lastName() {
        return LAST_NAMES.get(RND.nextInt(LAST_NAMES.size()));
    }
}
