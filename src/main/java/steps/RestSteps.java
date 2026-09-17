package steps;

import entity.ErrorResponse;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.Getter;
import org.hamcrest.Matchers;

import java.util.Map;

import static io.restassured.RestAssured.given;

public abstract class RestSteps {
    public RequestSpecification requestSpec;
    @Getter
    public Response response;

    public RestSteps(RequestSpecification spec) {
        this.requestSpec = spec;
    }

    public void sendPostWithParams(Map<String, String> params) {
        this.response = given(requestSpec)
            .params(params)
            .when()
            .post()
            .then()
            .extract()
            .response();
    }

    public void sendPostWithSpec(RequestSpecification customSpec, Map<String, String> params) {
        this.response = given(customSpec)
            .params(params)
            .when()
            .post()
            .then()
            .extract()
            .response();;
    }

    public ErrorResponse assertAndGetError(int statusCode) {
        Response response = getResponse();
        if (response == null) throw new IllegalStateException("Нет ответа от сервера");
        response.then().statusCode(Matchers.greaterThanOrEqualTo(statusCode));
        return response.as(ErrorResponse.class);
    }

    @Step("Проверяем успешность выполнения запроса (код 200)")
    public void assertSuccess() {
        Response response = getResponse();
        if (response == null) throw new IllegalStateException("Нет ответа от сервера");
        response.then().statusCode(200);
    }

}