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
    final static String NO_COLOR = "no";
    final static String CHERRY_COLOR = "red";
    final static String CHANGED_COLOR = "_changed";
    final static String CHANGED_COLOR_2ND = "_changed_2nd";
    public static final String NUTRI_NAME = "_not_changed_yet";

    @BeforeAll
    public static void enableLogging() {

        //
        // RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @Order(-1)
    public void initialDataSet() {
        Fruit fruit = new Fruit(CHERRY_UUID, false, CHERRY_NAME, NO_COLOR);
        fruit.addNutritions(new NutritionValue(UUID.randomUUID(), true, NUTRI_NAME, "y"));
        createNew(fruit);
        createNew(new Fruit(UUID.randomUUID(), false, "Apple", "green"));
        createNew(new Fruit(UUID.randomUUID(), false, "Banana", "yellow"));
    }

    @Test
    @Order(1)
    public void checkInitialDataSet() {

        //List all, should have min. 3 fruits the database has initially:
        final String oneValidId = given()
                .when().get("/fruits")
                .then()
                .statusCode(200)
                .body(containsString(NUTRI_NAME),
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
    public void getInitialSingleCherryEdited() {
        given()
                .when().get("/fruits/" + CHERRY_UUID)
                .then()
                .statusCode(200)
                .body("edited.ref.name", equalTo("Cherry"),
                        "active", Matchers.emptyOrNullString(),
                        "fetchDate", not(Matchers.emptyOrNullString())
                );
    }

    @Test
    @Order(3)
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
                .extract().jsonPath().getObject("edited.ref", Fruit.class);
        // now Change it with a put on /fruits/{id}
        final Fruit copy = baseFruit.copy();
        copy.color = CHANGED_COLOR;
        NutritionValue[] nutritionValues = copy.values.toArray(new NutritionValue[]{});
        nutritionValues[0].name = CHANGED_COLOR;
        given().with().body(copy).contentType(ContentType.JSON)
                .when().put("/fruits/" + CHERRY_UUID)
                .then()
                .statusCode(200)
                .body(
                        "name", equalTo(CHERRY_NAME),
                        "color", equalTo(CHANGED_COLOR),
                        "values[0].name", equalTo(CHANGED_COLOR));
    }

    @Test
    @Order(5)
    @DisplayName("edited version changed by update")
    public void checkActiveAfterChangeStillSame() {
        // do a GET to check active version values are still unchanged
        given()
                .when().get("/fruits/" + CHERRY_UUID)
                .then()
                .statusCode(200)
                .body(
                        "active", Matchers.emptyOrNullString(),
                        "edited.ref.name", equalTo(CHERRY_NAME),
                        "edited.ref.color", equalTo(CHANGED_COLOR)
                );
    }

    @Test
    @Order(6)
    @DisplayName("edited version associate changed by update")
    public void checkEditedIsThere() {
        // do a GET to check values are still as they were returned on the PUT
        given()
                .when().get("/fruits/" + CHERRY_UUID)
                .then()
                .statusCode(200)
                .body(
                        "edited.ref.name", equalTo(CHERRY_NAME),
                        "edited.ref.color", equalTo(CHANGED_COLOR),
                        "edited.ref.values[0].name", equalTo(CHANGED_COLOR)

                );
    }


    @Test
    @Order(11)
    @DisplayName("patch to new active version")
    public void newActiveCherryWithEditedColor() {

        given().with().body("{\"activeRevision\":true}").contentType(ContentType.JSON)
                .when().patch("/fruits/" + CHERRY_UUID)
                .then()
                .statusCode(200)
                .body(
                        "name", equalTo(CHERRY_NAME),
                        "color", equalTo(CHANGED_COLOR),
                        "values[0].name", equalTo(CHANGED_COLOR)

                );
    }

    @Test
    @Order(12)
    @DisplayName("check edited version after patch")
    public void afterPatchEdited() {
        // do a GET to check values are still as they were returned on the PUT
        given()
                .when().get("/fruits/" + CHERRY_UUID)
                .then()
                .statusCode(200)
                .body(
                        "edited", Matchers.emptyOrNullString());
    }


    @Test
    @Order(13)
    @DisplayName("check active version after patch")
    public void afterPatchActive() {
        // do a GET to check values are still as they were returned on the PUT
        given()
                .when().get("/fruits/" + CHERRY_UUID)
                .then()
                .statusCode(200)
                .body(
                        "active.ref.name", equalTo(CHERRY_NAME),
                        "active.ref.color", equalTo(CHANGED_COLOR),
                        "active.ref.values[0].name", equalTo(CHANGED_COLOR)
                );
    }

    @Test
    @Order(14)
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
        // now also set as edited version and use PUT
        copy.activeRevision = false;
        copy.editedRevision = null;
        given().with().body(copy).contentType(ContentType.JSON)
                .when().put("/fruits/" + CHERRY_UUID)
                .then()
                .statusCode(200)
                .body(
                        "name", equalTo(CHERRY_NAME),
                        "color", equalTo(CHANGED_COLOR_2ND));
    }

    @Test
    @Order(14)
    @DisplayName("activate")
    public void changeActiveCherryColor2nd() {

        final Fruit baseFruit = given()
                .when().get("/fruits/" + CHERRY_UUID)
                .then()
                .statusCode(200)
                .extract().jsonPath().getObject("edited.ref", Fruit.class);
        // now Change it with a put on /fruits/{id}
        baseFruit.color = CHANGED_COLOR_2ND;
        // now also set as edited version and use PUT
        baseFruit.activeRevision = true;
        given().with().body(baseFruit).contentType(ContentType.JSON)
                .when().put("/fruits/" + CHERRY_UUID)
                .then()
                .statusCode(200)
                .body(
                        "name", equalTo(CHERRY_NAME),
                        "color", equalTo(CHANGED_COLOR_2ND));
    }

    @Test
    @Order(18)
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
