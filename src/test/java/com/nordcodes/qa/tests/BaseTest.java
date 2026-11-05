package com.nordcodes.qa.tests;

import com.nordcodes.qa.client.EndpointClient;
import com.nordcodes.qa.env.EnvironmentExtension;
import com.nordcodes.qa.env.TestConfig;
import com.nordcodes.qa.env.TestEnvironment;
import com.nordcodes.qa.mock.ExternalServiceMock;
import com.nordcodes.qa.steps.Checks;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@Epic("Сервис internal: эндпоинт /endpoint")
@ExtendWith(EnvironmentExtension.class)
public abstract class BaseTest {

    protected EndpointClient app;
    protected ExternalServiceMock externalService;

    @BeforeEach
    void prepare() {
        TestEnvironment env = TestEnvironment.get();
        app = new EndpointClient(env.appUrl(), TestConfig.apiKey());
        externalService = new ExternalServiceMock(env.mock());
        externalService.worksNormally();
    }

    @Step("Предусловие: пользователь с токеном {token} выполнил вход (LOGIN)")
    protected void loggedIn(String token) {
        Checks.assertSuccess(app.login(token));
    }
}
