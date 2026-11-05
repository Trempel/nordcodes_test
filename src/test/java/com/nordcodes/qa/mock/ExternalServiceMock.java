package com.nordcodes.qa.mock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;

import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Заглушка внешнего сервиса (эндпоинты /auth и /doAction) на базе WireMock.
 * Позволяет задать, что ответит внешний сервис, и проверить, какие запросы к нему отправило приложение.
 */
public class ExternalServiceMock {

    public static final String AUTH = "/auth";
    public static final String DO_ACTION = "/doAction";

    private final WireMockServer server;

    public ExternalServiceMock(WireMockServer server) {
        this.server = server;
    }

    @Step("Подготовка: внешний сервис работает штатно (/auth и /doAction отвечают 200 OK)")
    public void worksNormally() {
        server.resetAll();
        respond(AUTH, 200, "{}");
        respond(DO_ACTION, 200, "{}");
    }

    @Step("Подготовка: внешний сервис на /auth отвечает кодом {status}")
    public void authResponds(int status) {
        respond(AUTH, status, "{}");
    }

    @Step("Подготовка: внешний сервис на /doAction отвечает кодом {status}")
    public void doActionResponds(int status) {
        respond(DO_ACTION, status, "{}");
    }

    @Step("Подготовка: внешний сервис на {path} отвечает кодом {status} и телом «{body}»")
    public void respondsWithBody(String path, int status, String body) {
        respond(path, status, body);
    }

    @Step("Подготовка: внешний сервис на {path} недоступен (соединение обрывается без ответа)")
    public void unavailable(String path) {
        server.stubFor(post(urlPathEqualTo(path))
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
    }

    @Step("Проверить: приложение отправило на /auth запросов с токеном {token}: {times}")
    public void verifyAuthCalls(String token, int times) {
        verifyCalls(AUTH, token, times);
    }

    @Step("Проверить: приложение отправило на /doAction запросов с токеном {token}: {times}")
    public void verifyDoActionCalls(String token, int times) {
        verifyCalls(DO_ACTION, token, times);
    }

    @Step("Проверить: приложение не обращалось к внешнему сервису")
    public void verifyNoCalls() {
        List<LoggedRequest> all = server.findAll(anyRequestedFor(anyUrl()));
        attachReceived(all);
        assertThat(all)
                .as("Запросы к внешнему сервису (их не должно быть)")
                .isEmpty();
    }

    @Step("Проверить: всего внешний сервис получил запросов: {times}")
    public void verifyTotalCalls(int times) {
        List<LoggedRequest> all = server.findAll(anyRequestedFor(anyUrl()));
        attachReceived(all);
        assertThat(all)
                .as("Общее число запросов к внешнему сервису")
                .hasSize(times);
    }

    /** Все запросы, которые внешний сервис получил на указанный адрес. */
    public List<LoggedRequest> receivedRequests(String path) {
        List<LoggedRequest> requests = server.findAll(postRequestedFor(urlPathEqualTo(path)));
        attachReceived(requests);
        return requests;
    }

    private void verifyCalls(String path, String token, int times) {
        RequestPatternBuilder pattern = postRequestedFor(urlPathEqualTo(path))
                .withRequestBody(equalTo("token=" + token));
        attachReceived(server.findAll(anyRequestedFor(anyUrl())));
        assertThat(server.findAll(pattern))
                .as("Число запросов на %s с токеном %s", path, token)
                .hasSize(times);
    }

    private void respond(String path, int status, String body) {
        server.stubFor(post(urlPathEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    private static void attachReceived(List<LoggedRequest> requests) {
        String text = requests.isEmpty()
                ? "Внешний сервис не получил ни одного запроса"
                : requests.stream()
                .map(r -> r.getMethod() + " " + r.getUrl() + "\n"
                        + r.getHeaders().all().stream()
                        .map(h -> h.key() + ": " + String.join(", ", h.values()))
                        .collect(Collectors.joining("\n"))
                        + "\n\n" + r.getBodyAsString())
                .collect(Collectors.joining("\n\n------------------------------\n\n"));
        Allure.addAttachment("Запросы, полученные внешним сервисом", "text/plain", text, ".txt");
    }
}
