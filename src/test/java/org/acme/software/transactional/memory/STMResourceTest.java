package org.acme.software.transactional.memory;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

@QuarkusTest
class STMResourceTest {

    @Test
    void testGet() {
        given()
                .when().get("/stm")
                .then()
                .statusCode(200);
    }

    @Test
    void testPost() {
        String responseString;

        makeBooking();
        responseString = makeBooking();

        assertThat(responseString, containsString("Booking Count=2"));
    }

    private String makeBooking() {
        return RestAssured.post("/stm").then()
                .assertThat()
                .statusCode(200)
                .extract()
                .asString();
    }
}
