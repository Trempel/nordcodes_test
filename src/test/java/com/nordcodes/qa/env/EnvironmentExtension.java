package com.nordcodes.qa.env;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Поднимает тестовое окружение перед первым тестовым классом и гасит его в конце прогона.
 */
public class EnvironmentExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        context.getRoot()
                .getStore(ExtensionContext.Namespace.GLOBAL)
                .getOrComputeIfAbsent(TestEnvironment.class, key -> TestEnvironment.startOnce(), TestEnvironment.class);
    }
}
