package com.nordcodes.qa.util;

import java.util.concurrent.ThreadLocalRandom;

/** Генерация токенов для тестов. Каждый тест получает свой уникальный токен, чтобы тесты не влияли друг на друга. */
public final class Tokens {

    public static final int LENGTH = 32;
    private static final String HEX_ALPHABET = "0123456789ABCDEF";
    private static final String SPEC_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private Tokens() {
    }

    /** Случайный корректный токен: 32 символа 0-9 и A-F (подходит под оба варианта правил). */
    public static String random() {
        return randomOf(HEX_ALPHABET, LENGTH);
    }

    /** Случайный токен из символов, разрешённых требованиями (A-Z, 0-9), с обязательными буквами G-Z. */
    public static String randomWithLettersBeyondF() {
        return "GHZ" + randomOf(SPEC_ALPHABET, LENGTH - 3);
    }

    public static String randomOf(String alphabet, int length) {
        StringBuilder sb = new StringBuilder(length);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
