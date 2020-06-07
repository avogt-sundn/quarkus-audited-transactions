package org.acme.hibernate.envers.panache;


import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RestAssuredJsonPathTest {
    final static String body = "[{\"id\":\"b2f40a42-9748-4111-9b53-c205879d607a\",\"name\":\"Cherry\"}," +
            "{\"id\":\"b2f40a42-9748-4111-9b53-c205879d607c\",\"name\":\"Apple\"}," +
            "{\"id\":\"b2f40a42-9748-4111-9b53-c205879d607e\",\"name\":\"Banana\"}]\n";

    @Test
    public void selectOne() {

        Assertions.assertEquals("b2f40a42-9748-4111-9b53-c205879d607a", JsonPath.from(body).getString(
                "find{ it.name == 'Cherry' }.id"));
    }
}
