package com.nordcodes.qa.steps;

import com.nordcodes.qa.client.ApiResponse;
import io.qameta.allure.Step;
import org.assertj.core.api.SoftAssertions;

/**
 * Проверки ответов приложения. Каждая проверка - отдельный шаг в отчёте с понятным описанием.
 */
public final class Checks {

    public static final String OK = "OK";
    public static final String ERROR = "ERROR";

    private Checks() {
    }

    @Step("Проверить: запрос выполнен успешно (HTTP 200, result = OK)")
    public static void assertSuccess(ApiResponse response) {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(response.statusCode()).as("HTTP-код ответа").isEqualTo(200);
        soft.assertThat(response.result()).as("Поле result в ответе").isEqualTo(OK);
        soft.assertThat(response.hasMessageField()).as("В успешном ответе не должно быть поля message").isFalse();
        soft.assertAll();
    }

    @Step("Проверить: запрос отклонён (HTTP {expectedStatus}, result = ERROR, есть причина ошибки)")
    public static void assertRejected(ApiResponse response, int expectedStatus) {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(response.statusCode()).as("HTTP-код ответа").isEqualTo(expectedStatus);
        checkErrorBody(soft, response);
        soft.assertAll();
    }

    @Step("Проверить: запрос не выполнен (result = ERROR, есть причина ошибки)")
    public static void assertFailed(ApiResponse response) {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(response.statusCode()).as("HTTP-код ответа должен сообщать об ошибке").isGreaterThanOrEqualTo(400);
        checkErrorBody(soft, response);
        soft.assertAll();
    }

    @Step("Проверить: причина ошибки содержит «{expectedText}»")
    public static void assertErrorMessageContains(ApiResponse response, String expectedText) {
        org.assertj.core.api.Assertions.assertThat(response.message())
                .as("Текст причины ошибки (message)")
                .contains(expectedText);
    }

    private static void checkErrorBody(SoftAssertions soft, ApiResponse response) {
        soft.assertThat(response.result()).as("Поле result в ответе").isEqualTo(ERROR);
        soft.assertThat(response.message()).as("Поле message с причиной ошибки").isNotBlank();
    }
}
