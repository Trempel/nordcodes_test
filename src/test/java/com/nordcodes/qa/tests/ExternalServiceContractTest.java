package com.nordcodes.qa.tests;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.nordcodes.qa.client.EndpointClient;
import com.nordcodes.qa.env.TestConfig;
import com.nordcodes.qa.mock.ExternalServiceMock;
import com.nordcodes.qa.steps.Checks;
import com.nordcodes.qa.util.Tokens;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Запросы к внешнему сервису")
@DisplayName("Запросы приложения к внешнему сервису")
class ExternalServiceContractTest extends BaseTest {

    @Test
    @Story("Запрос /auth")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("При входе приложение отправляет корректный запрос на /auth")
    @Description("Запрос на /auth должен соответствовать описанию: метод POST, "
            + "Content-Type: application/x-www-form-urlencoded, Accept: application/json, тело token=<токен>.")
    void authRequestFormat() {
        String token = Tokens.random();

        Checks.assertSuccess(app.login(token));

        List<LoggedRequest> requests = externalService.receivedRequests(ExternalServiceMock.AUTH);
        assertThat(requests).as("Число запросов на /auth").hasSize(1);
        checkRequestFormat(requests.get(0), token);
    }

    @Test
    @Story("Запрос /doAction")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("При действии приложение отправляет корректный запрос на /doAction")
    @Description("Запрос на /doAction должен соответствовать описанию: метод POST, "
            + "Content-Type: application/x-www-form-urlencoded, Accept: application/json, тело token=<токен>.")
    void doActionRequestFormat() {
        String token = Tokens.random();
        loggedIn(token);

        Checks.assertSuccess(app.action(token));

        List<LoggedRequest> requests = externalService.receivedRequests(ExternalServiceMock.DO_ACTION);
        assertThat(requests).as("Число запросов на /doAction").hasSize(1);
        checkRequestFormat(requests.get(0), token);
    }

    @Test
    @Story("Безопасность")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Секретный API-ключ не передаётся внешнему сервису")
    @Description("API-ключ приложения - секрет. Он не должен уходить во внешний сервис ни в заголовках, ни в теле запросов.")
    void apiKeyIsNotLeaked() {
        String token = Tokens.random();
        loggedIn(token);
        Checks.assertSuccess(app.action(token));

        List<LoggedRequest> requests = new ArrayList<>(externalService.receivedRequests(ExternalServiceMock.AUTH));
        requests.addAll(externalService.receivedRequests(ExternalServiceMock.DO_ACTION));
        checkNoApiKey(requests);
    }

    @Step("Проверить формат запроса: POST, нужные заголовки, тело token={token}")
    private void checkRequestFormat(LoggedRequest request, String token) {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(request.getMethod().getName()).as("Метод запроса").isEqualTo("POST");
        soft.assertThat(request.getHeader("Content-Type")).as("Заголовок Content-Type")
                .startsWith("application/x-www-form-urlencoded");
        soft.assertThat(request.getHeader("Accept")).as("Заголовок Accept").isEqualTo("application/json");
        soft.assertThat(request.getBodyAsString()).as("Тело запроса").isEqualTo("token=" + token);
        soft.assertAll();
    }

    @Step("Проверить: в запросах к внешнему сервису нет API-ключа")
    private void checkNoApiKey(List<LoggedRequest> requests) {
        SoftAssertions soft = new SoftAssertions();
        for (LoggedRequest request : requests) {
            soft.assertThat(request.containsHeader(EndpointClient.API_KEY_HEADER))
                    .as("Заголовок X-Api-Key в запросе %s", request.getUrl()).isFalse();
            soft.assertThat(request.getBodyAsString())
                    .as("Тело запроса %s", request.getUrl()).doesNotContain(TestConfig.apiKey());
        }
        soft.assertAll();
    }
}
