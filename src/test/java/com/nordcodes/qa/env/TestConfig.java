package com.nordcodes.qa.env;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Настройки тестов: читаются из test.properties, любое значение можно переопределить через -Dключ=значение.
 */
public final class TestConfig {

    private static final Properties PROPS = load();

    private TestConfig() {
    }

    public static boolean startApp() {
        return Boolean.parseBoolean(get("app.start"));
    }

    public static String appJar() {
        return get("app.jar");
    }

    public static String apiKey() {
        return get("app.secret");
    }

    public static int startupTimeoutSec() {
        return Integer.parseInt(get("app.startup.timeout.sec"));
    }

    public static String appUrl() {
        return get("app.url");
    }

    public static int mockPort() {
        return Integer.parseInt(get("mock.port"));
    }

    private static String get(String key) {
        String value = System.getProperty(key, PROPS.getProperty(key));
        if (value == null) {
            throw new IllegalStateException("Не задан параметр тестов: " + key);
        }
        return value.trim();
    }

    private static Properties load() {
        Properties props = new Properties();
        try (InputStream in = TestConfig.class.getClassLoader().getResourceAsStream("test.properties")) {
            if (in != null) {
                props.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return props;
    }
}
