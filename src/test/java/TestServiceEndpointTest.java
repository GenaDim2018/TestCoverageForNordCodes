import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import enums.Actions;
import io.qameta.allure.Feature;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import steps.TestServiceRestSteps;
import util.PropertyReader;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static util.Helpers.generateToken;
import static util.Helpers.withTokenAndAction;

@Feature("POST /endpoint")
public class TestServiceEndpointTest {
    private final TestServiceRestSteps testServiceRestSteps = new TestServiceRestSteps();
    private final String token = generateToken();
    @RegisterExtension
    static WireMockExtension mock = WireMockExtension.newInstance()
        .options(WireMockConfiguration.wireMockConfig()
            .bindAddress(PropertyReader.getProperty("external.service.url"))
            .port(PropertyReader.getIntProperty("external.service.port")))
        .build();

    private void stubPostEndpoint(String url) {
        mock.stubFor(post(urlEqualTo(url))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", ContentType.URLENC.toString())
                .withHeader("Accept", ContentType.JSON.toString())));
    }

    @Test
    @DisplayName("Успешный запрос авторизации: action=LOGIN")
    public void successAuthorization() {
        stubPostEndpoint("/auth");

        testServiceRestSteps.postEndpointRequest(withTokenAndAction(token, Actions.LOGIN.name()));

        testServiceRestSteps.assertSuccess();
        testServiceRestSteps.assertResultOk();
        testServiceRestSteps.verifyAuthRequestSent(mock, token);
    }

    @Test
    @DisplayName("Успешный запрос action: action=ACTION")
    public void successAction() {
        stubPostEndpoint("/auth");
        stubPostEndpoint("/doAction");
        testServiceRestSteps.postEndpointRequest(withTokenAndAction(token, Actions.LOGIN.name()));
        testServiceRestSteps.assertSuccess();

        testServiceRestSteps.postEndpointRequest(withTokenAndAction(token, Actions.ACTION.name()));

        testServiceRestSteps.assertSuccess();
        testServiceRestSteps.assertResultOk();
        testServiceRestSteps.verifyDoActionRequestSent(mock, token);
    }

    @Test
    @DisplayName("Успешный запрос logout: action=LOGOUT")
    public void successLogout() {
        stubPostEndpoint("/auth");
        testServiceRestSteps.postEndpointRequest(withTokenAndAction(token, Actions.LOGIN.name()));
        testServiceRestSteps.assertSuccess();

        testServiceRestSteps.postEndpointRequest(withTokenAndAction(token, Actions.LOGOUT.name()));

        testServiceRestSteps.assertSuccess();
        testServiceRestSteps.assertResultOk();
    }

    @Test
    @DisplayName("Успешный запрос action: action=ACTION, повторный запрос")
    public void successRepeatedAction() {
        stubPostEndpoint("/auth");
        stubPostEndpoint("/doAction");
        testServiceRestSteps.postEndpointRequest(withTokenAndAction(token, Actions.LOGIN.name()));
        testServiceRestSteps.assertSuccess();
        testServiceRestSteps.postEndpointRequest(withTokenAndAction(token, Actions.ACTION.name()));
        testServiceRestSteps.assertSuccess();
        testServiceRestSteps.assertResultOk();

        testServiceRestSteps.postEndpointRequest(withTokenAndAction(token, Actions.ACTION.name()));

        testServiceRestSteps.assertSuccess();
        testServiceRestSteps.assertResultOk();
    }

    @Test
    @DisplayName("Успешный повторный вход после выхода")
    public void successReloginAfterLogout() {
        stubPostEndpoint("/auth");
        testServiceRestSteps.postEndpointRequest(withTokenAndAction(token, Actions.LOGIN.name()));
        testServiceRestSteps.assertSuccess();
        testServiceRestSteps.postEndpointRequest(withTokenAndAction(token, Actions.LOGOUT.name()));
        testServiceRestSteps.assertSuccess();

        testServiceRestSteps.postEndpointRequest(withTokenAndAction(token, Actions.LOGIN.name()));

        testServiceRestSteps.assertSuccess();
        testServiceRestSteps.assertResultOk();
    }

    @Test
    @DisplayName("Неуспешный запрос logout: action=LOGOUT, отсутствует авторизация")
    public void failureLogoutNoLogin() {
        testServiceRestSteps.postEndpointRequest(withTokenAndAction(token, Actions.LOGOUT.name()));

        testServiceRestSteps.assertErrorMessageContains(403, "Token '%s' not found".formatted(token));
    }

    @Test
    @DisplayName("Неуспешный запрос action: action=ACTION, отсутствует авторизация")
    public void failureActionNoLogin() {
        stubPostEndpoint("/doAction");

        testServiceRestSteps.postEndpointRequest(withTokenAndAction(token, Actions.ACTION.name()));

        testServiceRestSteps.assertErrorMessageContains(403, "Token '%s' not found".formatted(token));
    }

    @Test
    @DisplayName("Неуспешный запрос logout: action=LOGOUT, повторная отправка")
    public void failureRepeatedLogout() {
        stubPostEndpoint("/auth");
        testServiceRestSteps.postEndpointRequest(withTokenAndAction(token, Actions.LOGIN.name()));
        testServiceRestSteps.assertSuccess();
        testServiceRestSteps.postEndpointRequest(withTokenAndAction(token, Actions.LOGOUT.name()));
        testServiceRestSteps.assertSuccess();

        testServiceRestSteps.postEndpointRequest(withTokenAndAction(token, Actions.LOGOUT.name()));

        testServiceRestSteps.assertErrorMessageContains(403, "Token '%s' not found".formatted(token));
    }

    @Test
    @DisplayName("Неуспешный запрос авторизации: action=LOGIN, повторная отправка")
    public void failureRepeatedLogin() {
        stubPostEndpoint("/auth");
        testServiceRestSteps.postEndpointRequest(withTokenAndAction(token, Actions.LOGIN.name()));
        testServiceRestSteps.assertSuccess();

        testServiceRestSteps.postEndpointRequest(withTokenAndAction(token, Actions.LOGIN.name()));

        testServiceRestSteps.assertErrorMessageContains(403, "Token '%s' already exists".formatted(token));
    }

    @Test
    @DisplayName("Неуспешный запрос авторизации: action=LOGIN, недоступен внешний сервис")
    public void failureLoginExternalServiceUnavailable() {
        testServiceRestSteps.postEndpointRequest(withTokenAndAction(token, Actions.LOGIN.name()));

        testServiceRestSteps.assertErrorMessageContains(500, "Internal Server Error");
    }

    @Test
    @DisplayName("Неуспешный запрос авторизации: action=LOGIN, внешний сервис не отвечает 10 секунд")
    public void failureLoginExternalServiceTimeout() {
        mock.stubFor(post(urlEqualTo("/auth"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", ContentType.URLENC.toString())
                .withHeader("Accept", ContentType.JSON.toString())
                .withFixedDelay(10000)));
        testServiceRestSteps.postEndpointRequest(withTokenAndAction(token, Actions.LOGIN.name()));

        testServiceRestSteps.assertErrorMessageContains(500, "Internal Server Error");
    }

    @Test
    @DisplayName("Неуспешный запрос action: action=ACTION, недоступен внешний сервис")
    public void failureActionExternalServiceUnavailable() {
        stubPostEndpoint("/auth");
        testServiceRestSteps.postEndpointRequest(withTokenAndAction(token, Actions.LOGIN.name()));
        testServiceRestSteps.assertSuccess();

        testServiceRestSteps.postEndpointRequest(withTokenAndAction(token, Actions.ACTION.name()));

        testServiceRestSteps.assertErrorMessageContains(500, "Internal Server Error");
    }

    @Test
    @DisplayName("Неуспешный запрос авторизации: action=ACTION, внешний сервис не отвечает 10 секунд)")
    public void failureActionExternalServiceTimeout() {
        stubPostEndpoint("/auth");
        mock.stubFor(post(urlEqualTo("/doAction"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", ContentType.URLENC.toString())
                .withHeader("Accept", ContentType.JSON.toString())
                .withFixedDelay(10000)));
        testServiceRestSteps.postEndpointRequest(withTokenAndAction(token, Actions.LOGIN.name()));
        testServiceRestSteps.assertSuccess();

        testServiceRestSteps.postEndpointRequest(withTokenAndAction(token, Actions.ACTION.name()));

        testServiceRestSteps.assertErrorMessageContains(500, "Internal Server Error");
    }

    @Test
    @DisplayName("Неуспешный запрос авторизации: action=UNKNOWN")
    public void failureIncorrectAction() {
        String action = "UNKNOWN";
        testServiceRestSteps.postEndpointRequest(withTokenAndAction(token, action));

        testServiceRestSteps.assertErrorMessageContains(400, "action: invalid action '%s'. Allowed: LOGIN, LOGOUT, ACTION".formatted(action));
    }

    @Test
    @DisplayName("Неуспешный запрос авторизации: не передан action")
    public void failureMissingParameterAction() {
        testServiceRestSteps.postEndpointRequest(Map.of("token", token));

        testServiceRestSteps.assertErrorMessageContains(400, "action: invalid action 'null'. Allowed: LOGIN, LOGOUT, ACTION");
    }

    @Test
    @DisplayName("Неуспешный запрос авторизации: Отсутствует параметр token")
    public void failureMissingTokenParameter() {
        testServiceRestSteps.postEndpointRequest(Map.of("action", Actions.LOGIN.name()));

        testServiceRestSteps.assertErrorMessageContains(400, "token: не должно равняться null");
    }

    @ParameterizedTest(name = "Неуспешный запрос авторизации: передан некорректный токен {0} ({1})")
    @DisplayName("Неуспешный запрос авторизации: action=LOGIN, некорректный токен")
    @CsvSource({
        "5707A5BC7FFA4D269F3CDBEC3708DE3, Короче 32 символов",
        "5707A5BC7FFA4D269F3CDBEC3708DE31A, Длиннее 32 символов",
        "'', Пустая строка",
        "5707A5BC7FFA4D269F3CDBEC3708De31, Нижний регистр",
        "5707A5BC7FFA4D269F3CDBEC3708DE3_, Спецсимвол",
    })
    public void failureTokenValidation(String token, String ignoredDescription) {
        testServiceRestSteps.postEndpointRequest(withTokenAndAction(token, Actions.LOGIN.name()));

        testServiceRestSteps.assertErrorMessageContains(400, "token: должно соответствовать \"^[0-9A-F]{32}$\"");
    }

    @Test
    @DisplayName("Неуспешный запрос действия: чужой токен не имеет доступа")
    public void failureActionWithDifferentToken() {
        String tokenA = generateToken();
        String tokenB = generateToken();
        stubPostEndpoint("/auth");
        stubPostEndpoint("/doAction");
        testServiceRestSteps.postEndpointRequest(withTokenAndAction(tokenA, Actions.LOGIN.name()));
        testServiceRestSteps.assertSuccess();

        testServiceRestSteps.postEndpointRequest(withTokenAndAction(tokenB, Actions.ACTION.name()));

        testServiceRestSteps.assertErrorMessageContains(403, "Token '%s' not found".formatted(tokenB));
    }

    @ParameterizedTest(name = "Неуспешный запрос авторизации: передан некорректный api key {0} ({1})")
    @CsvSource({
        "123, Некорректный api key",
        "'', Пустая строка"
    })
    public void failureApiKeyValidation(String apiKey, String ignoredDescription) {
        testServiceRestSteps.postEndPointRequestWithInvalidKey(apiKey, withTokenAndAction(token, Actions.LOGIN.name()));

        testServiceRestSteps.assertErrorMessageContains(401, "Missing or invalid API Key");
    }

}
