package steps;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import entity.ErrorResponse;
import entity.TestServiceResponse;
import io.qameta.allure.Step;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Assertions;
import specs.TestServiceReqSpec;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

public class TestServiceRestSteps extends RestSteps {
    public TestServiceRestSteps() {
        super(TestServiceReqSpec.testServiceEndpointReqSpec());
    }

    @Step("Выполняем запрос POST /endpoint с параметрами '{params}'")
    public void postEndpointRequest(Map<String, String> params) {
        super.sendPostWithParams(params);
    }

    @Step("Проверяем ошибку '{statusCode}' и текст '{expectedMessage}'")
    public void assertErrorMessageContains(int statusCode, String expectedMessage) {
        ErrorResponse error = assertAndGetError(statusCode);
        Assertions.assertEquals("ERROR", error.getResult(), "Некорректный результат в ответе");
        Assertions.assertEquals(expectedMessage, error.getMessage(), "Некорректный текст ошибки");
    }

    @Step("Проверяем в теле ответа result\": \"OK\" ")
    public void assertResultOk() {
        TestServiceResponse testServiceResponse = response.as(TestServiceResponse.class);
        Assertions.assertEquals("OK", testServiceResponse.getResult(), "Некорректный результат в ответе");
    }

    @Step("Выполняем запрос с неверным API-ключом: {apiKey}")
    public TestServiceRestSteps postEndPointRequestWithInvalidKey(String apiKey, Map<String, String> params) {
        RequestSpecification invalidKeySpec = TestServiceReqSpec.testServiceEndpointReqSpecWithHeaders(Map.of("x-api-key", apiKey));
        super.sendPostWithSpec(invalidKeySpec, params);
        return this;
    }

    @Step("Проверяем, что запрос авторизации во внешний сервис был отправлен с токеном {token}")
    public void verifyAuthRequestSent(WireMockExtension mock, String token) {
        mock.verify(postRequestedFor(urlEqualTo("/auth"))
            .withRequestBody(containing("token=" + token)));
    }

    @Step("Проверяем, что запрос действия во внешний сервис был отправлен с токеном {token}")
    public void verifyDoActionRequestSent(WireMockExtension mock, String token) {
        mock.verify(postRequestedFor(urlEqualTo("/doAction"))
            .withRequestBody(containing("token=" + token)));
    }

    @Step("Проверяем, что во внешний сервис не было обращений")
    public void verifyNoExternalCalls(WireMockExtension mock) {
        mock.verify(0, anyRequestedFor(anyUrl()));
    }

}