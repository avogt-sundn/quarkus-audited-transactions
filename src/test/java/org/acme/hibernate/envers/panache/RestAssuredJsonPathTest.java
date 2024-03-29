package org.acme.hibernate.envers.panache;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.restassured.path.json.JsonPath;

/**
 * This test helps to understand RestAssured use and make sure it still works as
 * the test developers assumed it would when they wrote the other tests
 * initially.
 */
class RestAssuredJsonPathTest {
    final static String body = "[{\"id\":\"b2f40a42-9748-4111-9b53-c205879d607a\",\"name\":\"Cherry\"}," +
            "{\"id\":\"b2f40a42-9748-4111-9b53-c205879d607c\",\"name\":\"Apple\"}," +
            "{\"id\":\"b2f40a42-9748-4111-9b53-c205879d607e\",\"name\":\"Banana\"}]\n";

    @Test
    void selectOne() {

        Assertions.assertEquals("b2f40a42-9748-4111-9b53-c205879d607a",
                JsonPath.from(RestAssuredJsonPathTest.body).getString(
                        "find{ it.name == 'Cherry' }.id"));
    }
}
