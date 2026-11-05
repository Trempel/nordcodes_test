package com.nordcodes.qa.tests;

import com.nordcodes.qa.client.ApiResponse;
import com.nordcodes.qa.mock.ExternalServiceMock;
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

@Feature("Вход: LOGIN")
@DisplayName("Вход пользователя (LOGIN)")
class LoginTest extends BaseTest {

    @Test
    @Story("Успешный вход")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Успешный вход: внешний сервис подтвердил токен")
    @Description("Пользователь отправляет LOGIN, приложение проверяет токен во внешнем сервисе (/auth). "
            + "Внешний сервис отвечает успехом - приложение отвечает OK.")
    void successfulLogin() {
        String token = Tokens.random();

        ApiResponse response = app.login(token);

        Checks.assertSuccess(response);
        externalService.verifyAuthCalls(token, 1);
    }

    @Test
    @Story("Успешный вход")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("После успешного входа пользователю доступно действие ACTION")
    @Description("Успешный LOGIN сохраняет токен: после него запрос ACTION с тем же токеном выполняется успешно.")
    void loginOpensSession() {
        String token = Tokens.random();

        Checks.assertSuccess(app.login(token));
        ApiResponse action = app.action(token);

        Checks.assertSuccess(action);
    }

    @ParameterizedTest(name = "Внешний сервис ответил «{0}»")
    @ValueSource(strings = {"{}", "{\"status\":\"error\"}", "не JSON, просто текст", ""})
    @Story("Успешный вход")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Тело ответа внешнего сервиса не влияет на результат входа")
    @Description("По требованиям приложение смотрит только на код ответа внешнего сервиса. "
            + "При коде 200 вход успешен, что бы ни было в теле ответа.")
    void loginIgnoresAuthResponseBody(String body) {
        String token = Tokens.random();
        externalService.respondsWithBody(ExternalServiceMock.AUTH, 200, body);

        ApiResponse response = app.login(token);

        Checks.assertSuccess(response);
    }

    @ParameterizedTest(name = "/auth ответил HTTP {0}")
    @ValueSource(ints = {400, 401, 403, 404, 409, 500, 502, 503})
    @Story("Внешний сервис отказал во входе")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Вход не выполняется, если внешний сервис ответил ошибкой")
    @Description("Если внешний сервис (/auth) отвечает кодом ошибки, приложение отвечает ERROR "
            + "и НЕ сохраняет токен: последующий ACTION с этим токеном не проходит и не доходит до внешнего сервиса.")
    void loginFailsWhenAuthReturnsError(int status) {
        String token = Tokens.random();
        externalService.authResponds(status);

        ApiResponse response = app.login(token);

        Checks.assertFailed(response);
        Checks.assertFailed(app.action(token));
        externalService.verifyDoActionCalls(token, 0);
    }

    @Test
    @Story("Внешний сервис отказал во входе")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Вход не выполняется, если внешний сервис недоступен")
    @Description("Если соединение с внешним сервисом обрывается, приложение отвечает ERROR и не сохраняет токен.")
    void loginFailsWhenAuthUnavailable() {
        String token = Tokens.random();
        externalService.unavailable(ExternalServiceMock.AUTH);

        ApiResponse response = app.login(token);

        Checks.assertFailed(response);
        Checks.assertFailed(app.action(token));
        externalService.verifyDoActionCalls(token, 0);
    }

    @Test
    @Story("Внешний сервис отказал во входе")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("После неудачного входа можно войти снова, когда внешний сервис восстановился")
    @Description("Неудачная попытка входа не «блокирует» токен: когда внешний сервис снова отвечает успехом, "
            + "повторный LOGIN с тем же токеном проходит.")
    void loginRetryAfterAuthFailure() {
        String token = Tokens.random();
        externalService.authResponds(500);
        Checks.assertFailed(app.login(token));

        externalService.authResponds(200);
        ApiResponse retry = app.login(token);

        Checks.assertSuccess(retry);
        externalService.verifyAuthCalls(token, 2);
    }

    @Test
    @Story("Повторный вход")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Повторный вход с уже вошедшим токеном отклоняется")
    @Description("Если токен уже прошёл LOGIN, повторный LOGIN возвращает ошибку, "
            + "внешний сервис повторно не вызывается, а уже открытая сессия продолжает работать.")
    void repeatedLoginIsRejected() {
        String token = Tokens.random();
        loggedIn(token);

        ApiResponse second = app.login(token);

        Checks.assertFailed(second);
        externalService.verifyAuthCalls(token, 1);
        Checks.assertSuccess(app.action(token));
    }

    @Test
    @Story("Несколько пользователей")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Разные пользователи входят независимо друг от друга")
    @Description("Вход одного пользователя не даёт доступа другому: ACTION для токена, который не выполнял LOGIN, "
            + "отклоняется, даже если другой токен уже вошёл.")
    void sessionsAreIndependent() {
        String first = Tokens.random();
        String second = Tokens.random();
        loggedIn(first);

        Checks.assertSuccess(app.action(first));
        Checks.assertFailed(app.action(second));
        externalService.verifyDoActionCalls(second, 0);
    }
}
