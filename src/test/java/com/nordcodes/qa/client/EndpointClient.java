package com.nordcodes.qa.client;

import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * Клиент тестируемого приложения: POST /endpoint c параметрами token и action.
 * Каждый метод - шаг отчёта Allure, а запрос и ответ прикладываются к шагу целиком.
 */
public class EndpointClient {

    public static final String API_KEY_HEADER = "X-Api-Key";
    private static final String PATH = "/endpoint";

    private final String baseUrl;
    private final String apiKey;

    public EndpointClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    @Step("Отправить запрос LOGIN (вход) с токеном {token}")
    public ApiResponse login(String token) {
        return post(apiKey, token, Action.LOGIN.name());
    }

    @Step("Отправить запрос ACTION (действие) с токеном {token}")
    public ApiResponse action(String token) {
        return post(apiKey, token, Action.ACTION.name());
    }

    @Step("Отправить запрос LOGOUT (выход) с токеном {token}")
    public ApiResponse logout(String token) {
        return post(apiKey, token, Action.LOGOUT.name());
    }

    @Step("Отправить запрос: token = «{token}», action = «{action}»")
    public ApiResponse send(String token, String action) {
        return post(apiKey, token, action);
    }

    @Step("Отправить запрос LOGIN с API-ключом «{key}»")
    public ApiResponse loginWithApiKey(String key, String token) {
        return post(key, token, Action.LOGIN.name());
    }

    @Step("Отправить запрос LOGIN без заголовка X-Api-Key")
    public ApiResponse loginWithoutApiKey(String token) {
        return post(null, token, Action.LOGIN.name());
    }

    @Step("Отправить запрос с телом в формате {contentType}")
    public ApiResponse postRaw(ContentType contentType, String body) {
        return new ApiResponse(request(apiKey).contentType(contentType).body(body).post(PATH));
    }

    @Step("Отправить GET-запрос вместо POST")
    public ApiResponse get() {
        return new ApiResponse(request(apiKey).get(PATH));
    }

    private ApiResponse post(String key, String token, String action) {
        RequestSpecification spec = request(key).contentType(ContentType.URLENC);
        if (token != null) {
            spec.formParam("token", token);
        }
        if (action != null) {
            spec.formParam("action", action);
        }
        return new ApiResponse(spec.post(PATH));
    }

    private RequestSpecification request(String key) {
        RequestSpecification spec = RestAssured.given()
                .config(RestAssuredConfig.config().encoderConfig(
                        EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
                .baseUri(baseUrl)
                .accept(ContentType.JSON)
                .filter(new AllureRestAssured()
                        .setRequestAttachmentName("Запрос к приложению")
                        .setResponseAttachmentName("Ответ приложения"));
        if (key != null) {
            spec.header(API_KEY_HEADER, key);
        }
        return spec;
    }
}
