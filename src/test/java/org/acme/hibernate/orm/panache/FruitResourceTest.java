package org.acme.hibernate.orm.panache;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

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
    final String toBeSafe = "b2f40a42-9748-4111-9b53-c205879d607e";

    @BeforeAll
    public static void enableLogging() {

        //
        // RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @Order(1)
    public void checkTheImportResults() {
        given()
                .when().get("/fruits")
                .then()
                .statusCode(200)
                // at least the import.sql entities should account for
                .body("uuid", Matchers.iterableWithSize(Matchers.greaterThan(2)));
        //List all, should have all 3 fruits the database has initially:
        final String oneValidId = given()
                .when().get("/fruits")
                .then()
                .statusCode(200)
                .body(
                        containsString("Cherry"),
                        containsString("Apple"),
                        containsString("Banana")
                )
                // 'find' is gpath special keyword introduncing a filter
                // 'it' is a gpath special keyword referencing the current node
                // 'name' and 'id' are the field names from the fruit class that got serialized to json
                .extract().body().jsonPath().getString("find{ it.name == 'Banana' }.uuid");

        Assertions.assertEquals(toBeSafe, oneValidId);
    }

    @Test
    @Order(2)
    public void getSingleFruit() {
        given()
                .when().get("/fruits/" + toBeSafe)
                .then()
                .statusCode(200)
                .body(containsString("Banana"));
    }

    @Test
    @Order(3)
    public void testDeleteSingleFruit() {
        final String toBeDeleted = "b2f40a42-9748-4111-9b53-c205879d607b";

        //Delete the Cherry:
        given()
                .when().delete("/fruits/" + toBeDeleted)
                .then()
                .statusCode(204);
        given()
                .when().get("/fruits/" + toBeDeleted)
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

    @Test
    @Order(4)
    public void change() {
        // we use the fruit we know is there due to import.sql, with the uuid 'toBeChanged'
        final String toBeChanged = "b2f40a42-9748-4111-9b53-c205879d607c";

        final Fruit baseVersion = given()
                .when().get("/fruits/" + toBeChanged)
                .then()
                .statusCode(200)
                .extract().body().as(Fruit.class);

        // now Change it with a put on /fruits/{id}
        final Fruit copy = baseVersion.copy();
        copy.name = "changed";
        given().with().body(copy).contentType(ContentType.JSON)
                .when().put("/fruits/" + toBeChanged)
                .then()
                .statusCode(201)
                .body(
                        "name", equalTo(copy.name),
                        "color", equalTo(copy.color),
                        "version", equalTo(baseVersion.version + 1));

        // do a GET to check values are still as they were returned on the PUT
        given()
                .when().get("/fruits/" + toBeChanged)
                .then()
                .statusCode(200)
                .body(
                        "name", equalTo("changed"),
                        "color", equalTo(copy.color),
                        "version", equalTo(baseVersion.version + 1));
    }

    @Test
    @Order(5)
    public void createNew() {
        //List all, cherry should be missing now:
        given().with().body(new Fruit("name")).contentType(ContentType.JSON)
                .when().post("/fruits")
                .then()
                .statusCode(201)
                .extract().body().as(Fruit.class).getName().equals("name");
    }


}
