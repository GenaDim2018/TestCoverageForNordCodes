package specs;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import util.PropertyReader;

import java.util.Map;

public class TestServiceReqSpec {
    private final static String BASE_URI = PropertyReader.getProperty("app.base.url");
    private final static String BASE_PATH = "endpoint";
    private final static String X_API_KEY = PropertyReader.getProperty("x.api.key");

    public static RequestSpecification testServiceEndpointReqSpec() {
        return new RequestSpecBuilder()
            .addFilter(new AllureRestAssured())
            .setBaseUri(BASE_URI)
            .setBasePath(BASE_PATH)
            .setContentType(ContentType.URLENC)
            .setAccept(ContentType.JSON)
            .addHeader("x-api-key", X_API_KEY)
            .build();
    }

    public static RequestSpecification testServiceEndpointReqSpecWithHeaders(Map<String, String> headers) {
        return new RequestSpecBuilder()
            .addFilter(new AllureRestAssured())
            .setBaseUri(BASE_URI)
            .setBasePath(BASE_PATH)
            .setContentType(ContentType.URLENC)
            .setAccept(ContentType.JSON)
            .addHeaders(headers)
            .build();
    }
}
