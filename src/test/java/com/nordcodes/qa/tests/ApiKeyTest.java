package com.nordcodes.qa.tests;

import com.nordcodes.qa.client.ApiResponse;
import com.nordcodes.qa.steps.Checks;
import com.nordcodes.qa.util.Tokens;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Feature("Доступ по API-ключу")
@DisplayName("Доступ по API-ключу (заголовок X-Api-Key)")
class ApiKeyTest extends BaseTest {

    @Test
    @Story("Верный ключ")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Запрос с верным API-ключом принимается")
    @Description("Приложение обрабатывает запрос, если в заголовке X-Api-Key передан правильный ключ.")
    void requestWithValidKeyIsAccepted() {
        String token = Tokens.random();

        ApiResponse response = app.login(token);

        Checks.assertSuccess(response);
    }

    @Test
    @Story("Отсутствует ключ")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Запрос без API-ключа отклоняется")
    @Description("Без заголовка X-Api-Key приложение должно отказать в доступе (HTTP 401) "
            + "и не выполнять действие: внешний сервис не вызывается, вход не выполняется.")
    void requestWithoutKeyIsRejected() {
        String token = Tokens.random();

        ApiResponse response = app.loginWithoutApiKey(token);

        Checks.assertRejected(response, 401);
        externalService.verifyNoCalls();
    }

    @ParameterizedTest(name = "Неверный ключ «{0}»")
    @ValueSource(strings = {"wrongKey", "qazwsxedc", "QAZWSXEDC", "qazWSXed", "qazWSXedcc", ""})
    @Story("Неверный ключ")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Запрос с неверным API-ключом отклоняется")
    @Description("Ключ должен совпадать точно: другой ключ, другой регистр букв, ключ короче или длиннее "
            + "и пустой ключ - всё это отказ в доступе (HTTP 401) без обращения к внешнему сервису.")
    void requestWithWrongKeyIsRejected(String wrongKey) {
        String token = Tokens.random();

        ApiResponse response = app.loginWithApiKey(wrongKey, token);

        Checks.assertRejected(response, 401);
        externalService.verifyNoCalls();
    }

    @Test
    @Story("Неверный ключ")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Запрос с неверным ключом не создаёт сессию")
    @Description("После отклонённого LOGIN с неверным ключом токен не должен считаться вошедшим: "
            + "ACTION с правильным ключом для этого токена должен завершиться ошибкой.")
    void rejectedLoginDoesNotCreateSession() {
        String token = Tokens.random();

        Checks.assertRejected(app.loginWithApiKey("wrongKey", token), 401);
        ApiResponse action = app.action(token);

        Checks.assertFailed(action);
        externalService.verifyNoCalls();
    }
}
