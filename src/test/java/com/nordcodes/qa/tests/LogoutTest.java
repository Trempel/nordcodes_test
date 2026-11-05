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

@Feature("Выход: LOGOUT")
@DisplayName("Выход пользователя (LOGOUT)")
class LogoutTest extends BaseTest {

    @Test
    @Story("Успешный выход")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Вошедший пользователь успешно выходит")
    @Description("LOGOUT для вошедшего пользователя возвращает OK. Выход выполняется внутри приложения: "
            + "к внешнему сервису обращений нет (кроме /auth при самом входе).")
    void logoutAfterLogin() {
        String token = Tokens.random();
        loggedIn(token);

        ApiResponse response = app.logout(token);

        Checks.assertSuccess(response);
        externalService.verifyTotalCalls(1);
    }

    @Test
    @Story("Успешный выход")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("После выхода можно снова войти с тем же токеном")
    @Description("LOGOUT удаляет токен из хранилища, поэтому повторный LOGIN с тем же токеном проходит как новый вход.")
    void loginAgainAfterLogout() {
        String token = Tokens.random();
        loggedIn(token);
        Checks.assertSuccess(app.logout(token));

        ApiResponse relogin = app.login(token);

        Checks.assertSuccess(relogin);
        Checks.assertSuccess(app.action(token));
        externalService.verifyAuthCalls(token, 2);
    }

    @Test
    @Story("Выход без входа")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Выход без предварительного входа возвращает ошибку")
    @Description("Для токена, который не выполнял LOGIN, LOGOUT возвращает ERROR.")
    void logoutWithoutLogin() {
        String token = Tokens.random();

        ApiResponse response = app.logout(token);

        Checks.assertFailed(response);
        externalService.verifyNoCalls();
    }

    @Test
    @Story("Выход без входа")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Повторный выход возвращает ошибку")
    @Description("Первый LOGOUT успешен, второй подряд - ошибка, потому что токен уже удалён.")
    void doubleLogout() {
        String token = Tokens.random();
        loggedIn(token);
        Checks.assertSuccess(app.logout(token));

        ApiResponse second = app.logout(token);

        Checks.assertFailed(second);
    }

    @Test
    @Story("Несколько пользователей")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Выход одного пользователя не затрагивает других")
    @Description("Двое пользователей вошли, один вышел. Второй продолжает выполнять действия.")
    void logoutDoesNotAffectOtherSessions() {
        String leaving = Tokens.random();
        String staying = Tokens.random();
        loggedIn(leaving);
        loggedIn(staying);

        Checks.assertSuccess(app.logout(leaving));

        Checks.assertSuccess(app.action(staying));
        Checks.assertFailed(app.action(leaving));
    }

    @Test
    @Story("Успешный выход")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Выход работает, даже если внешний сервис недоступен")
    @Description("LOGOUT не зависит от внешнего сервиса: если сервис упал после входа, выйти всё равно можно.")
    void logoutDoesNotDependOnExternalService() {
        String token = Tokens.random();
        loggedIn(token);
        externalService.authResponds(503);
        externalService.doActionResponds(503);

        Checks.assertSuccess(app.logout(token));
    }
}
