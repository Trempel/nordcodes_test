package com.nordcodes.qa.client;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.util.Map;

/**
 * Ответ тестируемого приложения в удобном для проверок виде.
 */
public final class ApiResponse {

    private final int statusCode;
    private final String contentType;
    private final String body;
    private final String result;
    private final String message;
    private final boolean hasMessageField;

    ApiResponse(Response response) {
        this.statusCode = response.statusCode();
        this.contentType = response.contentType();
        this.body = response.asString();
        String parsedResult = null;
        String parsedMessage = null;
        boolean messagePresent = false;
        try {
            JsonPath json = JsonPath.from(body);
            parsedResult = json.getString("result");
            Object root = json.get();
            messagePresent = root instanceof Map && ((Map<?, ?>) root).containsKey("message");
            parsedMessage = json.getString("message");
        } catch (RuntimeException notJson) {
            // тело ответа не JSON - поля останутся пустыми, проверки это покажут
        }
        this.result = parsedResult;
        this.message = parsedMessage;
        this.hasMessageField = messagePresent;
    }

    public int statusCode() {
        return statusCode;
    }

    public String contentType() {
        return contentType;
    }

    public String body() {
        return body;
    }

    public String result() {
        return result;
    }

    public String message() {
        return message;
    }

    public boolean hasMessageField() {
        return hasMessageField;
    }

    @Override
    public String toString() {
        return "HTTP " + statusCode + " " + body;
    }
}
