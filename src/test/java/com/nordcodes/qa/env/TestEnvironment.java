package com.nordcodes.qa.env;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.extension.ExtensionContext;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Тестовое окружение: заглушка внешнего сервиса (WireMock) и тестируемое приложение.
 * Создаётся один раз на весь прогон и останавливается после всех тестов.
 */
public final class TestEnvironment implements ExtensionContext.Store.CloseableResource {

    private static volatile TestEnvironment current;

    private final WireMockServer mock;
    private final ApplicationProcess app;
    private final String appUrl;

    private TestEnvironment() {
        boolean startApp = TestConfig.startApp();

        mock = new WireMockServer(options()
                .port(startApp ? 0 : TestConfig.mockPort())
                .bindAddress("127.0.0.1"));
        mock.start();

        if (startApp) {
            String mockUrl = "http://localhost:" + mock.port() + "/";
            app = ApplicationProcess.start(TestConfig.appJar(), TestConfig.apiKey(), mockUrl,
                    TestConfig.startupTimeoutSec());
            appUrl = app.baseUrl();
        } else {
            app = null;
            appUrl = TestConfig.appUrl();
        }
    }

    static synchronized TestEnvironment startOnce() {
        if (current == null) {
            current = new TestEnvironment();
        }
        return current;
    }

    public static TestEnvironment get() {
        if (current == null) {
            throw new IllegalStateException("Тестовое окружение не запущено: тест должен быть помечен @ExtendWith(EnvironmentExtension.class)");
        }
        return current;
    }

    public WireMockServer mock() {
        return mock;
    }

    public String appUrl() {
        return appUrl;
    }

    @Override
    public void close() {
        try {
            if (app != null) {
                app.stop();
            }
        } finally {
            mock.stop();
            current = null;
        }
    }
}
