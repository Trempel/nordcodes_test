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

@Feature("Действие: ACTION")
@DisplayName("Действие пользователя (ACTION)")
class ActionTest extends BaseTest {

    @Test
    @Story("Успешное действие")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Вошедший пользователь успешно выполняет действие")
    @Description("После LOGIN запрос ACTION отправляется во внешний сервис (/doAction). "
            + "Внешний сервис отвечает успехом - приложение отвечает OK.")
    void actionAfterLogin() {
        String token = Tokens.random();
        loggedIn(token);

        ApiResponse response = app.action(token);

        Checks.assertSuccess(response);
        externalService.verifyDoActionCalls(token, 1);
    }

    @Test
    @Story("Успешное действие")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Действие можно выполнять несколько раз подряд")
    @Description("Сессия не закрывается после действия: три ACTION подряд выполняются успешно, "
            + "и каждый раз приложение обращается к внешнему сервису.")
    void severalActionsInOneSession() {
        String token = Tokens.random();
        loggedIn(token);

        Checks.assertSuccess(app.action(token));
        Checks.assertSuccess(app.action(token));
        Checks.assertSuccess(app.action(token));

        externalService.verifyDoActionCalls(token, 3);
    }

    @ParameterizedTest(name = "Внешний сервис ответил «{0}»")
    @ValueSource(strings = {"{}", "{\"status\":\"error\"}", "не JSON, просто текст", ""})
    @Story("Успешное действие")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Тело ответа внешнего сервиса не влияет на результат действия")
    @Description("По требованиям приложение смотрит только на код ответа. При коде 200 действие успешно, "
            + "что бы ни было в теле ответа.")
    void actionIgnoresResponseBody(String body) {
        String token = Tokens.random();
        loggedIn(token);
        externalService.respondsWithBody(ExternalServiceMock.DO_ACTION, 200, body);

        Checks.assertSuccess(app.action(token));
    }

    @Test
    @Story("Действие без входа")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Действие без предварительного входа запрещено")
    @Description("ACTION доступен только после LOGIN. Для токена, не выполнявшего вход, приложение возвращает ошибку "
            + "и не обращается к внешнему сервису.")
    void actionWithoutLoginIsRejected() {
        String token = Tokens.random();

        ApiResponse response = app.action(token);

        Checks.assertFailed(response);
        externalService.verifyNoCalls();
    }

    @Test
    @Story("Действие без входа")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Действие после выхода запрещено")
    @Description("После LOGOUT токен удаляется: ACTION с ним возвращает ошибку и не доходит до внешнего сервиса.")
    void actionAfterLogoutIsRejected() {
        String token = Tokens.random();
        loggedIn(token);
        Checks.assertSuccess(app.logout(token));

        ApiResponse response = app.action(token);

        Checks.assertFailed(response);
        externalService.verifyDoActionCalls(token, 0);
    }

    @ParameterizedTest(name = "/doAction ответил HTTP {0}")
    @ValueSource(ints = {400, 401, 403, 404, 500, 502, 503})
    @Story("Ошибка внешнего сервиса")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Действие не выполняется, если внешний сервис ответил ошибкой")
    @Description("Если внешний сервис (/doAction) отвечает кодом ошибки, приложение сообщает пользователю об ошибке (ERROR).")
    void actionFailsWhenDoActionReturnsError(int status) {
        String token = Tokens.random();
        loggedIn(token);
        externalService.doActionResponds(status);

        ApiResponse response = app.action(token);

        Checks.assertFailed(response);
        externalService.verifyDoActionCalls(token, 1);
    }

    @Test
    @Story("Ошибка внешнего сервиса")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Действие не выполняется, если внешний сервис недоступен")
    @Description("Если соединение с внешним сервисом обрывается, приложение отвечает ERROR.")
    void actionFailsWhenDoActionUnavailable() {
        String token = Tokens.random();
        loggedIn(token);
        externalService.unavailable(ExternalServiceMock.DO_ACTION);

        Checks.assertFailed(app.action(token));
    }

    @Test
    @Story("Ошибка внешнего сервиса")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Ошибка внешнего сервиса не закрывает сессию пользователя")
    @Description("Неудачный ACTION не разлогинивает пользователя: когда внешний сервис восстановился, "
            + "следующий ACTION с тем же токеном выполняется успешно без повторного входа.")
    void sessionSurvivesDoActionFailure() {
        String token = Tokens.random();
        loggedIn(token);
        externalService.doActionResponds(500);
        Checks.assertFailed(app.action(token));

        externalService.doActionResponds(200);
        ApiResponse retry = app.action(token);

        Checks.assertSuccess(retry);
        externalService.verifyAuthCalls(token, 1);
    }
}
