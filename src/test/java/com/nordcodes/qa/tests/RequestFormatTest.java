package com.nordcodes.qa.tests;

import com.nordcodes.qa.client.ApiResponse;
import com.nordcodes.qa.steps.Checks;
import com.nordcodes.qa.util.Tokens;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Формат запроса и ответа")
@DisplayName("Формат запроса и ответа")
class RequestFormatTest extends BaseTest {

    @Test
    @Story("Формат ответа")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Успешный ответ приходит в формате JSON")
    @Description("Ответ на успешный запрос имеет тип application/json и тело {\"result\": \"OK\"}.")
    void successResponseIsJson() {
        ApiResponse response = app.login(Tokens.random());

        checkJsonContentType(response);
        Checks.assertSuccess(response);
    }

    @Test
    @Story("Формат ответа")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Ответ с ошибкой приходит в формате JSON с причиной")
    @Description("Ответ на ошибочный запрос имеет тип application/json и тело "
            + "{\"result\": \"ERROR\", \"message\": \"<причина>\"}.")
    void errorResponseIsJson() {
        ApiResponse response = app.action(Tokens.random());

        checkJsonContentType(response);
        Checks.assertFailed(response);
    }

    @Test
    @Tag("known-bug")
    @Story("Неподдерживаемый формат запроса")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("Запрос в формате JSON вместо формы отклоняется как ошибка клиента")
    @Description("Эндпоинт принимает только данные формы (application/x-www-form-urlencoded). "
            + "На запрос в другом формате ожидается понятная ошибка клиента (код 4xx, например 415). "
            + "ИЗВЕСТНЫЙ ДЕФЕКТ: приложение отвечает 500 «Internal Server Error», как будто сломалось само.")
    void jsonBodyIsClientError() {
        String body = "{\"token\":\"" + Tokens.random() + "\",\"action\":\"LOGIN\"}";

        ApiResponse response = app.postRaw(ContentType.JSON, body);

        checkClientError(response);
        externalService.verifyNoCalls();
    }

    @Test
    @Tag("known-bug")
    @Story("Неподдерживаемый формат запроса")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("GET-запрос отклоняется как ошибка клиента")
    @Description("Эндпоинт принимает только POST. На GET ожидается ошибка клиента (код 4xx, например 405). "
            + "ИЗВЕСТНЫЙ ДЕФЕКТ: приложение отвечает 500 «Internal Server Error».")
    void getMethodIsClientError() {
        ApiResponse response = app.get();

        checkClientError(response);
        externalService.verifyNoCalls();
    }

    @Step("Проверить: тип ответа - application/json")
    private void checkJsonContentType(ApiResponse response) {
        assertThat(response.contentType()).as("Тип содержимого ответа (Content-Type)").startsWith("application/json");
    }

    @Step("Проверить: ответ - ошибка клиента (код 4xx), result = ERROR")
    private void checkClientError(ApiResponse response) {
        assertThat(response.statusCode())
                .as("[ДЕФЕКТ] Ожидалась ошибка клиента (4xx), а пришёл код %d. Ответ приложения: %s",
                        response.statusCode(), response)
                .isBetween(400, 499);
        assertThat(response.result()).as("Поле result").isEqualTo(Checks.ERROR);
    }
}
