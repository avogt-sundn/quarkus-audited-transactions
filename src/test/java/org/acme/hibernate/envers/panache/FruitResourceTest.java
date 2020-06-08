package org.acme.hibernate.envers.panache;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsNot.not;

/**
 * Test the /fruits/ resource with GET,POST,PUT,DELETE
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FruitResourceTest {

    /**
     * the test will create a new Fruit to this uuid and use it throughout all test methods.
     * no assumptions about the contents of the database are made which makes it robust when
     * running in a suite with other QuarkusTest tests.
     */
    final static UUID CHERRY_UUID = UUID.randomUUID();
    final static String CHERRY_NAME = "Cherry";
    final static String CHERRY_COLOR = "red";
    final static String CHANGED_COLOR = "changed";
    final static String CHANGED_COLOR_2ND = "changed_2nd";

    @BeforeAll
    public static void enableLogging() {

        //
        // RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @Order(-1)
    public void initialDataSet() {
        createNew(new Fruit(CHERRY_UUID, true, CHERRY_NAME, CHERRY_COLOR));
        createNew(new Fruit(UUID.randomUUID(), true, "Apple", "green"));
        createNew(new Fruit(UUID.randomUUID(), true, "Banana", "yellow"));
    }

    @Test
    @Order(1)
    public void checkInitialDataSet() {

        //List all, should have min. 3 fruits the database has initially:
        final String oneValidId = given()
                .when().get("/fruits")
                .then()
                .statusCode(200)
                .body(
                        containsString(CHERRY_NAME),
                        containsString("Apple"),
                        containsString("Banana")
                )
                // 'find' is gpath special keyword introduncing a filter
                // 'it' is a gpath special keyword referencing the current node
                // 'name' and 'id' are the field names from the fruit class that got serialized to json
                .extract().body().jsonPath().getString("find{ it.name == '" + CHERRY_NAME + "' }.id");

        Assertions.assertEquals(CHERRY_UUID.toString(), oneValidId);
    }

    @Test
    @Order(2)
    public void getSingleFruit() {
        final String fullResponseBody =
                "{\n" +
                        "    \"active\": {\n" +
                        "        \"info\": {\n" +
                        "            \"id\": 1,\n" +
                        "            \"revisionDate\": \"2020-06-06T19:39:56\",\n" +
                        "            \"timestamp\": 1591472396218,\n" +
                        "            \"username\": \"your-name-Sat Jun 06 21:39:56 CEST 2020\"\n" +
                        "        },\n" +
                        "        \"ref\": {\n" +
                        "            \"active\": true,\n" +
                        "            \"color\": \"red\",\n" +
                        "            \"id\": \"a309e0bd-5181-4521-9e3c-e57b4eafe404\",\n" +
                        "            \"name\": \"Cherry\"\n" +
                        "        },\n" +
                        "        \"revision\": 1\n" +
                        "    },\n" +
                        "    \"fetchDate\": \"2020-06-06T21:39:57\"\n" +
                        "}";
        given()
                .when().get("/fruits/" + CHERRY_UUID)
                .then()
                .statusCode(200)
                .body("active.ref.name", equalTo("Cherry"),
                        "active.revision", equalTo(1),
                        "active.info.id", equalTo(1),
                        "fetchDate", not(Matchers.emptyOrNullString())
                );
    }

    @Test
    @Order(2)
    public void getSingleNonExistentFruit() {
        given()
                .when().get("/fruits/" + UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @Order(4)
    @DisplayName("create edited version")
    public void changeCherryColor() {

        final Fruit baseFruit = given()
                .when().get("/fruits/" + CHERRY_UUID)
                .then()
                .statusCode(200)
                .extract().jsonPath().getObject("active.ref", Fruit.class);
        // now Change it with a put on /fruits/{id}
        final Fruit copy = baseFruit.copy();
        copy.color = CHANGED_COLOR;
        given().with().body(copy).contentType(ContentType.JSON)
                .when().put("/fruits/" + CHERRY_UUID)
                .then()
                .statusCode(201)
                .body(
                        "name", equalTo(CHERRY_NAME),
                        "color", equalTo(CHANGED_COLOR));
    }

    @Test
    @Order(5)
    @DisplayName("active version unchanged by update")
    public void checkActiveAfterChangeStillSame() {
        // do a GET to check active version values are still unchanged
        given()
                .when().get("/fruits/" + CHERRY_UUID)
                .then()
                .statusCode(200)
                .body(
                        "active.ref.name", equalTo(CHERRY_NAME),
                        "active.ref.color", not(equalTo(CHANGED_COLOR)),
                        "active.ref.color", equalTo(CHERRY_COLOR)
                );
    }

    @Test
    @Order(6)
    @DisplayName("check edited version after update")
    public void checkEditedIsThere() {
        // do a GET to check values are still as they were returned on the PUT
        given()
                .when().get("/fruits/" + CHERRY_UUID)
                .then()
                .statusCode(200)
                .body(
                        "edited.ref.name", equalTo(CHERRY_NAME),
                        "edited.ref.color", equalTo(CHANGED_COLOR)
                );
    }


    @Test
    @Order(8)
    @DisplayName("new active version")
    public void newActiveCherryWithEditedColor() {

        given().with().body("{\"active\":true}").contentType(ContentType.JSON)
                .when().patch("/fruits/" + CHERRY_UUID)
                .then()
                .statusCode(201)
                .body(
                        "name", equalTo(CHERRY_NAME),
                        "color", equalTo(CHANGED_COLOR)
                );
    }

    @Test
    @Order(9)
    @DisplayName("check edited version after update")
    public void checkNoEditedNow() {
        // do a GET to check values are still as they were returned on the PUT
        given()
                .when().get("/fruits/" + CHERRY_UUID)
                .then()
                .statusCode(200)
                .body(
                        "edited", Matchers.emptyOrNullString());
    }

    @Test
    @Order(10)
    @DisplayName("create 2nd edited version")
    public void changeCherryColor2nd() {

        final Fruit baseFruit = given()
                .when().get("/fruits/" + CHERRY_UUID)
                .then()
                .statusCode(200)
                .extract().jsonPath().getObject("active.ref", Fruit.class);
        // now Change it with a put on /fruits/{id}
        final Fruit copy = baseFruit.copy();
        copy.color = CHANGED_COLOR_2ND;
        // now also set as active version and use PUT
        copy.active = true;
        given().with().body(copy).contentType(ContentType.JSON)
                .when().put("/fruits/" + CHERRY_UUID)
                .then()
                .statusCode(201)
                .body(
                        "name", equalTo(CHERRY_NAME),
                        "color", equalTo(CHANGED_COLOR_2ND));
    }

    @Test
    @Order(11)
    @DisplayName("check edited version after update")
    public void checkNoEditedNow2nd() {
        // do a GET to check values are still as they were returned on the PUT
        given()
                .when().get("/fruits/" + CHERRY_UUID)
                .then()
                .statusCode(200)
                .body(
                        "edited", Matchers.emptyOrNullString(),
                        "active.ref.name", equalTo(CHERRY_NAME),
                        "active.ref.color", equalTo(CHANGED_COLOR_2ND)
                );
    }

    @Test
    @Order(13)
    public void testDeleteSingleFruit() {

        //Delete the Cherry:
        given()
                .when().delete("/fruits/" + CHERRY_UUID)
                .then()
                .statusCode(204);
        given()
                .when().get("/fruits/" + CHERRY_UUID)
                .then()
                .statusCode(404);
        given()
                .when().get("/fruits")
                .then()
                .statusCode(200)
                .body(
                        not(containsString("Cherry")),
                        containsString("Apple"),
                        containsString("Banana"));
    }


    void createNew(Fruit fruit) {
        //List all, cherry should be missing now:
        given().with().body(fruit).contentType(ContentType.JSON)
                .when().post("/fruits")
                .then()
                .statusCode(201)
                .extract().body().as(Fruit.class).getName().equals(fruit.name);
    }


}