package com.nordcodes.qa.tests;

import com.nordcodes.qa.steps.Checks;
import com.nordcodes.qa.util.Tokens;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Feature("Сквозные сценарии")
@DisplayName("Сквозной сценарий работы пользователя")
class UserScenarioTest extends BaseTest {

    @Test
    @Story("Полный цикл сессии")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Вход → два действия → выход → действие запрещено")
    @Description("Типичный путь пользователя от начала до конца: вошёл, выполнил действия, вышел. "
            + "После выхода действия больше недоступны. Проверяем, что внешний сервис получил ровно "
            + "один запрос /auth и два запроса /doAction.")
    void fullSessionLifecycle() {
        String token = Tokens.random();

        Checks.assertSuccess(app.login(token));
        Checks.assertSuccess(app.action(token));
        Checks.assertSuccess(app.action(token));
        Checks.assertSuccess(app.logout(token));
        Checks.assertFailed(app.action(token));

        externalService.verifyAuthCalls(token, 1);
        externalService.verifyDoActionCalls(token, 2);
        externalService.verifyTotalCalls(3);
    }
}
