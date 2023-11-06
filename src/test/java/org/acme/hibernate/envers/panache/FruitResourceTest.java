package org.acme.hibernate.envers.panache;

import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

/**
 * Test the /fruits/ resource with GET,POST,PUT,DELETE
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FruitResourceTest {

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
    static final String NUTRI_NAME = "_not_changed_yet";

    @BeforeAll
    static void enableLogging() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
    @BeforeAll
    static void configureRestAssured() {
    }


    @Test
    @Order(-1)
    void initialDataSet() {
        Fruit fruit = new Fruit(FruitResourceTest.CHERRY_UUID, true, FruitResourceTest.CHERRY_NAME,
                FruitResourceTest.NO_COLOR);
        fruit.addNutritions(new NutritionValue(UUID.randomUUID(), true, FruitResourceTest.NUTRI_NAME, "use-nutrition"));
        createNew(fruit);
        createNew(new Fruit(UUID.randomUUID(), true, "Apple", "green"));
        createNew(new Fruit(UUID.randomUUID(), true, "Banana", "yellow"));

    }

    @Inject
    ObjectMapper jacksObjectMapper;

@Test
    @Order(-1)
    void checkJacksonSetup() {
        Assertions.assertTrue(this.jacksObjectMapper.isEnabled(JsonReadFeature.ALLOW_JAVA_COMMENTS.mappedFeature()));
    }

    @Test
    @Order(1)
    void checkInitialDataSet() {

        //List all, should have min. 3 fruits the database has initially:
        final String oneValidId = RestAssured.given()
                .when().get("/fruits")
                .then()
                .statusCode(200)
                .body(
                    CoreMatchers.containsString(FruitResourceTest.NUTRI_NAME),
                        CoreMatchers.containsString(FruitResourceTest.CHERRY_NAME),
                        CoreMatchers.containsString("Apple"),
                        CoreMatchers.containsString("Banana")
                )
                // 'find' is gpath special keyword introduncing a filter
                // 'it' is a gpath special keyword referencing the current node
                // 'name' and 'id' are the field names from the fruit class that got serialized to json
                .extract().body().jsonPath().getString("find{ it.name == '" + FruitResourceTest.CHERRY_NAME + "' }.id");

        Assertions.assertEquals(FruitResourceTest.CHERRY_UUID.toString(), oneValidId);
    }


    @Test
    @Order(2)
    void getInitialSingleCherryEdited() {
        RestAssured.given()
                .when().get("/fruits/" + FruitResourceTest.CHERRY_UUID)
                .then()
                .statusCode(200)
                .body("edited.ref.name", CoreMatchers.equalTo("Cherry"),
                        "active", Matchers.emptyOrNullString(),
                        "fetchDate", IsNot.not(Matchers.emptyOrNullString())
                );
    }

    @Test
    @Order(3)
    void getSingleNonExistentFruit() {
        RestAssured.given()
                .when().get("/fruits/" + UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @Order(4)
    @DisplayName("create edited version")
    void changeCherryColor() {

        final Fruit baseFruit = RestAssured.given()
                .when().get("/fruits/" + FruitResourceTest.CHERRY_UUID)
                .then()
                .statusCode(200)
                .extract().jsonPath().getObject("edited.ref", Fruit.class);
        // now Change it with a put on /fruits/{id}
        final Fruit copy = baseFruit.copy();
        copy.color = FruitResourceTest.CHANGED_COLOR;
        NutritionValue[] nutritionValues = copy.values.toArray(new NutritionValue[]{});
        nutritionValues[0].name = FruitResourceTest.CHANGED_COLOR;
        RestAssured.given().with().body(copy).contentType(ContentType.JSON)
                .when().put("/fruits/" + FruitResourceTest.CHERRY_UUID)
                .then()
                .statusCode(200)
                .body(
                        "name", CoreMatchers.equalTo(FruitResourceTest.CHERRY_NAME),
                        "color", CoreMatchers.equalTo(FruitResourceTest.CHANGED_COLOR),
                        "values[0].name", CoreMatchers.equalTo(FruitResourceTest.CHANGED_COLOR));
    }

    @Test
    @Order(5)
    @DisplayName("edited version changed by update")
    void checkActiveAfterChangeStillSame() {
        // do a GET to check active version values are still unchanged
        RestAssured.given()
                .when().get("/fruits/" + FruitResourceTest.CHERRY_UUID)
                .then()
                .statusCode(200)
                .body(
                        "active", Matchers.emptyOrNullString(),
                        "edited.ref.name", CoreMatchers.equalTo(FruitResourceTest.CHERRY_NAME),
                        "edited.ref.color", CoreMatchers.equalTo(FruitResourceTest.CHANGED_COLOR)
                );
    }

    @Test
    @Order(6)
    @DisplayName("edited version associate changed by update")
    void checkEditedIsThere() {
        // do a GET to check values are still as they were returned on the PUT
        RestAssured.given()
                .when().get("/fruits/" + FruitResourceTest.CHERRY_UUID)
                .then()
                .statusCode(200)
                .body(
                        "edited.ref.name", CoreMatchers.equalTo(FruitResourceTest.CHERRY_NAME),
                        "edited.ref.color", CoreMatchers.equalTo(FruitResourceTest.CHANGED_COLOR),
                        "edited.ref.values[0].name", CoreMatchers.equalTo(FruitResourceTest.CHANGED_COLOR)

                );
    }


    @Test
    @Order(11)
    @DisplayName("patch to new active version")
    void newActiveCherryWithEditedColor() {

        RestAssured.given().with().body("{\"activeRevision\":true}").contentType(ContentType.JSON)
                .when().patch("/fruits/" + FruitResourceTest.CHERRY_UUID)
                .then()
                .statusCode(200)
                .body(
                        "name", CoreMatchers.equalTo(FruitResourceTest.CHERRY_NAME),
                        "color", CoreMatchers.equalTo(FruitResourceTest.CHANGED_COLOR),
                        "values[0].name", CoreMatchers.equalTo(FruitResourceTest.CHANGED_COLOR)

                );
    }

    @Test
    @Order(12)
    @DisplayName("check edited version after patch")
    void afterPatchEdited() {
        // do a GET to check values are still as they were returned on the PUT
        RestAssured.given()
                .when().get("/fruits/" + FruitResourceTest.CHERRY_UUID)
                .then()
                .statusCode(200)
                .body(
                        "edited", Matchers.emptyOrNullString());
    }


    @Test
    @Order(13)
    @DisplayName("check active version after patch")
    void afterPatchActive() {
        // do a GET to check values are still as they were returned on the PUT
        RestAssured.given()
                .when().get("/fruits/" + FruitResourceTest.CHERRY_UUID)
                .then()
                .statusCode(200)
                .body(
                        "active.ref.name", CoreMatchers.equalTo(FruitResourceTest.CHERRY_NAME),
                        "active.ref.color", CoreMatchers.equalTo(FruitResourceTest.CHANGED_COLOR),
                        "active.ref.values[0].name", CoreMatchers.equalTo(FruitResourceTest.CHANGED_COLOR)
                );
    }

    @Test
    @Order(14)
    @DisplayName("create 2nd edited version")
    void changeCherryColor2nd() {

        final Fruit baseFruit = RestAssured.given()
                .when().get("/fruits/" + FruitResourceTest.CHERRY_UUID)
                .then()
                .statusCode(200)
                .extract().jsonPath().getObject("active.ref", Fruit.class);
        // now Change it with a put on /fruits/{id}
        final Fruit copy = baseFruit.copy();
        copy.color = FruitResourceTest.CHANGED_COLOR_2ND;
        // now also set as edited version and use PUT
        copy.activeRevision = false;
        copy.editedRevision = null;
        RestAssured.given().with().body(copy).contentType(ContentType.JSON)
                .when().put("/fruits/" + FruitResourceTest.CHERRY_UUID)
                .then()
                .statusCode(200)
                .body(
                        "name", CoreMatchers.equalTo(FruitResourceTest.CHERRY_NAME),
                        "color", CoreMatchers.equalTo(FruitResourceTest.CHANGED_COLOR_2ND));
    }

    @Test
    @Order(14)
    @DisplayName("activate")
    void changeActiveCherryColor2nd() {

        final Fruit baseFruit = RestAssured.given()
                .when().get("/fruits/" + FruitResourceTest.CHERRY_UUID)
                .then()
                .statusCode(200)
                .extract().jsonPath().getObject("edited.ref", Fruit.class);
        // now Change it with a put on /fruits/{id}
        baseFruit.color = FruitResourceTest.CHANGED_COLOR_2ND;
        // now also set as edited version and use PUT
        baseFruit.activeRevision = true;
        RestAssured.given().with().body(baseFruit).contentType(ContentType.JSON)
                .when().put("/fruits/" + FruitResourceTest.CHERRY_UUID)
                .then()
                .statusCode(200)
                .body(
                        "name", CoreMatchers.equalTo(FruitResourceTest.CHERRY_NAME),
                        "color", CoreMatchers.equalTo(FruitResourceTest.CHANGED_COLOR_2ND));
    }

    @Test
    @Order(18)
    void testDeleteSingleFruit() {

        //Delete the Cherry:
        RestAssured.given()
                .when().delete("/fruits/" + FruitResourceTest.CHERRY_UUID)
                .then()
                .statusCode(204);
        RestAssured.given()
                .when().get("/fruits/" + FruitResourceTest.CHERRY_UUID)
                .then()
                .statusCode(404);
        RestAssured.given()
                .when().get("/fruits")
                .then()
                .statusCode(200)
                .body(
                        IsNot.not(CoreMatchers.containsString("Cherry")),
                        CoreMatchers.containsString("Apple"),
                        CoreMatchers.containsString("Banana"));

    }


    void createNew(Fruit fruit) {
        //List all, cherry should be missing now:
        RestAssured.given().with().body(fruit).contentType(ContentType.JSON)
                .when().post("/fruits")
                .then()
                .statusCode(201)
                .extract().body().as(Fruit.class).getName().equals(fruit.name);
    }


}
