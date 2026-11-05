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
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Feature("Проверка входных данных")
@DisplayName("Проверка входных данных: token и action")
class RequestValidationTest extends BaseTest {

    static Stream<Arguments> validTokens() {
        return Stream.of(
                arguments(Named.of("только цифры", Tokens.randomOf("0123456789", 32))),
                arguments(Named.of("только буквы", Tokens.randomOf("ABCDEF", 32))),
                arguments(Named.of("цифры и буквы вперемешку", Tokens.random())),
                arguments(Named.of("все нули", "0".repeat(32))));
    }

    @ParameterizedTest(name = "Токен: {0}")
    @MethodSource("validTokens")
    @Story("Корректный токен")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Корректный токен из 32 символов принимается")
    @Description("Токен длиной ровно 32 символа из заглавных латинских букв и цифр принимается приложением.")
    void validTokenIsAccepted(String token) {
        ApiResponse response = app.login(token);

        Checks.assertSuccess(response);
        externalService.verifyAuthCalls(token, 1);
    }

    @Test
    @Tag("known-bug")
    @Story("Корректный токен")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Токен с буквами G–Z принимается (по требованиям допустимы A–Z)")
    @Description("По требованиям токен состоит из символов A-Z и 0-9. "
            + "ИЗВЕСТНЫЙ ДЕФЕКТ: приложение принимает только буквы A-F (шестнадцатеричный формат) "
            + "и отклоняет корректные по требованиям токены с буквами G-Z. Тест упадёт, пока дефект не исправлен.")
    void tokenWithLettersBeyondFIsAccepted() {
        String token = Tokens.randomWithLettersBeyondF();

        ApiResponse response = app.login(token);

        assertThat(response.result())
                .as("[ДЕФЕКТ] Токен %s соответствует требованиям (A-Z, 0-9), но отклонён. Ответ приложения: %s",
                        token, response)
                .isEqualTo(Checks.OK);
    }

    static Stream<Arguments> invalidTokens() {
        String valid = Tokens.random();
        return Stream.of(
                arguments(Named.of("31 символ (короче на 1)", valid.substring(1))),
                arguments(Named.of("33 символа (длиннее на 1)", valid + "A")),
                arguments(Named.of("пустая строка", "")),
                arguments(Named.of("строчные буквы", valid.toLowerCase().replaceAll("[0-9]", "a"))),
                arguments(Named.of("спецсимвол в конце", valid.substring(1) + "!")),
                arguments(Named.of("пробел внутри", valid.substring(0, 16) + " " + valid.substring(17))),
                arguments(Named.of("кириллическая буква «А»", valid.substring(1) + "А")),
                arguments(Named.of("дефис", valid.substring(1) + "-")));
    }

    @ParameterizedTest(name = "Токен: {0}")
    @MethodSource("invalidTokens")
    @Story("Некорректный токен")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Некорректный токен отклоняется")
    @Description("Если токен не соответствует формату (32 символа A-Z, 0-9), приложение возвращает ошибку "
            + "(HTTP 400) с описанием причины и не обращается к внешнему сервису.")
    void invalidTokenIsRejected(String token) {
        ApiResponse response = app.login(token);

        Checks.assertRejected(response, 400);
        externalService.verifyNoCalls();
    }

    @Test
    @Story("Некорректный токен")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Запрос без параметра token отклоняется")
    @Description("Параметр token обязателен. Без него приложение возвращает ошибку (HTTP 400).")
    void missingTokenIsRejected() {
        ApiResponse response = app.send(null, "LOGIN");

        Checks.assertRejected(response, 400);
        externalService.verifyNoCalls();
    }

    @ParameterizedTest(name = "action = «{0}»")
    @ValueSource(strings = {"login", "Login", "LOG_IN", "DELETE", "ACTIONS", ""})
    @Story("Некорректное действие")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Неизвестное действие отклоняется")
    @Description("Допустимы только действия LOGIN, ACTION и LOGOUT (заглавными буквами). "
            + "На любое другое значение приложение отвечает ошибкой (HTTP 400) и не обращается к внешнему сервису.")
    void unknownActionIsRejected(String action) {
        ApiResponse response = app.send(Tokens.random(), action);

        Checks.assertRejected(response, 400);
        externalService.verifyNoCalls();
    }

    @Test
    @Story("Некорректное действие")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Запрос без параметра action отклоняется")
    @Description("Параметр action обязателен. Без него приложение возвращает ошибку (HTTP 400).")
    void missingActionIsRejected() {
        ApiResponse response = app.send(Tokens.random(), null);

        Checks.assertRejected(response, 400);
        externalService.verifyNoCalls();
    }

    @Test
    @Story("Некорректный запрос")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Запрос без параметров отклоняется")
    @Description("Пустой запрос (нет ни token, ни action) отклоняется с ошибкой (HTTP 400).")
    void emptyRequestIsRejected() {
        ApiResponse response = app.send(null, null);

        Checks.assertRejected(response, 400);
        externalService.verifyNoCalls();
    }
}
