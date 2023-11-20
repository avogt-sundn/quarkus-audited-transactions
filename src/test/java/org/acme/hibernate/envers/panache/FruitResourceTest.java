package org.acme.hibernate.envers.panache;

import static org.hamcrest.Matchers.*;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Test the /fruits/ resource with GET,POST,PUT,DELETE
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
class FruitResourceTest {

    private final static UUID RANDOM_UUID = UUID.randomUUID();
    private final static String NUTRITION_IS_HERE = "nutrition-is-here";
    /**
     * the test will create a new Fruit to this uuid and use it throughout all test
     * methods. no assumptions about the contents of the database are made which
     * makes it robust when running in a suite with other QuarkusTest tests.
     */
    private final static UUID CHERRY_UUID = UUID.randomUUID();
    final static String CHERRY_NAME = "Cherry";
    final static String NO_COLOR = "no";
    final static String CHERRY_COLOR = "red";
    final static String CHANGED_COLOR = "_changed_color";
    final static String CHANGED_COLOR_2ND = "_2nd_changed_color";
    static final String CHERRY_NUTRI_NAME = "cherry_nutri_name";

    @BeforeAll
    static void initAll() {

        RestAssured.config = RestAssured.config.objectMapperConfig(ObjectMapperConfig
                .objectMapperConfig().defaultObjectMapperType(ObjectMapperType.JACKSON_2));

        LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails();

    }

    @Inject
    ObjectMapper objectMapper;

    @Test
    @Order(-1)
    void quarkusApplicationIsUsingJackson() {
        Assertions.assertTrue(
                this.objectMapper.isEnabled(JsonReadFeature.ALLOW_JAVA_COMMENTS.mappedFeature()));
    }

    @Test
    @Order(-1)
    void jupiterTestIsUsingJackson() {
        Assertions.assertEquals(ObjectMapperType.JACKSON_2,
                RestAssured.config().getObjectMapperConfig().defaultObjectMapperType());
    }

    @Test
    @Order(2)
    void initialDataSet() throws JsonProcessingException {
        Fruit fruit = new Fruit(CHERRY_UUID, true, CHERRY_NAME, FruitResourceTest.NO_COLOR);
        fruit.addNutritions(new NutritionValue(RANDOM_UUID, true,
                FruitResourceTest.CHERRY_NUTRI_NAME, NUTRITION_IS_HERE));
        createNew(fruit);

        NutritionValue nutri = (NutritionValue) NutritionValue.findById(RANDOM_UUID);
        Assertions.assertNotNull(nutri);
        Assertions.assertNotNull(nutri.fruit);
        Assertions.assertEquals(CHERRY_UUID, nutri.fruit.id);

        Fruit readFromDB = (Fruit) Fruit.findById(CHERRY_UUID);
        Assertions.assertEquals(1, readFromDB.values.size());
    }

    @Test
    @Order(3)
    void checkInitialDataSet() {
        log.info("nohist");
        // List all, should have min. 3 fruits the database has initially:
        RestAssured.given().when().get("/fruits/" + CHERRY_UUID + "/nohist").then().statusCode(200)
                .and().body("id", equalTo(CHERRY_UUID.toString()));

    }

    @Test
    @Order(3)
    void getSingleNonExistentFruit() {
        RestAssured.given().when().get("/fruits/" + UUID.randomUUID()).then().statusCode(404);
    }

    @Test
    @Order(4)
    @DisplayName("create edited version")
    void changeCherryColor() {

        final Fruit baseFruit = RestAssured.given().when().get("/fruits/" + CHERRY_UUID).then()
                .statusCode(200).extract().jsonPath().getObject("active.ref", Fruit.class);
        // now Change it with a put on /fruits/{id}
        final Fruit copy = baseFruit.copy();
        copy.color = CHANGED_COLOR;
        NutritionValue[] nutritionValues = copy.values.toArray(new NutritionValue[] {});
        nutritionValues[0].name = CHANGED_COLOR;
        RestAssured.given().with().body(copy).contentType(ContentType.JSON).when()
                .put("/fruits/" + CHERRY_UUID).then().statusCode(200).body("name",
                        CoreMatchers.equalTo(CHERRY_NAME), "color",
                        CoreMatchers.equalTo(CHANGED_COLOR), "values[0].name",
                        CoreMatchers.equalTo(CHANGED_COLOR));
    }

    @Test
    @Order(5)
    @DisplayName("edited version changed by update")
    void checkActiveAfterChangeStillSame() {
        // do a GET to check active version values are still unchanged
        RestAssured.given().when().get("/fruits/" + CHERRY_UUID).then().statusCode(200).body(
                "active", Matchers.emptyOrNullString(), "edited.ref.name",
                CoreMatchers.equalTo(CHERRY_NAME), "edited.ref.color",
                CoreMatchers.equalTo(CHANGED_COLOR));
    }

    @Test
    @Order(6)
    @DisplayName("edited version associate changed by update")
    void checkEditedIsThere() {
        // do a GET to check values are still as they were returned on the PUT
        RestAssured.given().when().get("/fruits/" + CHERRY_UUID).then().statusCode(200).body(
                "edited.ref.name", CoreMatchers.equalTo(CHERRY_NAME), "edited.ref.color",
                CoreMatchers.equalTo(CHANGED_COLOR), "edited.ref.values[0].name",
                CoreMatchers.equalTo(CHANGED_COLOR)

        );
    }

    @Test
    @Order(11)
    @DisplayName("patch to new active version")
    void newActiveCherryWithEditedColor() {

        RestAssured.given().with().body("{\"activeRevision\":true}").contentType(ContentType.JSON)
                .when().patch("/fruits/" + CHERRY_UUID).then().statusCode(200).body("name",
                        CoreMatchers.equalTo(CHERRY_NAME), "color",
                        CoreMatchers.equalTo(CHANGED_COLOR), "values[0].name",
                        CoreMatchers.equalTo(CHANGED_COLOR)

                );
    }

    @Test
    @Order(12)
    @DisplayName("check edited version after patch")
    void afterPatchEdited() {
        // do a GET to check values are still as they were returned on the PUT
        RestAssured.given().when().get("/fruits/" + CHERRY_UUID).then().statusCode(200)
                .body("edited", Matchers.emptyOrNullString());
    }

    @Test
    @Order(13)
    @DisplayName("check active version after patch")
    void afterPatchActive() {
        // do a GET to check values are still as they were returned on the PUT
        RestAssured.given().when().get("/fruits/" + CHERRY_UUID).then().statusCode(200).body(
                "active.ref.name", CoreMatchers.equalTo(CHERRY_NAME), "active.ref.color",
                CoreMatchers.equalTo(CHANGED_COLOR), "active.ref.values[0].name",
                CoreMatchers.equalTo(CHANGED_COLOR));
    }

    @Test
    @Order(14)
    @DisplayName("create 2nd edited version")
    void changeCherryColor2nd() {

        final Fruit baseFruit = RestAssured.given().when().get("/fruits/" + CHERRY_UUID).then()
                .statusCode(200).extract().jsonPath().getObject("active.ref", Fruit.class);
        // now Change it with a put on /fruits/{id}
        final Fruit copy = baseFruit.copy();
        copy.color = CHANGED_COLOR_2ND;
        // now also set as edited version and use PUT
        copy.activeRevision = false;
        copy.editedRevision = null;
        RestAssured.given().with().body(copy).contentType(ContentType.JSON).when()
                .put("/fruits/" + CHERRY_UUID).then().statusCode(200).body("name",
                        CoreMatchers.equalTo(CHERRY_NAME), "color",
                        CoreMatchers.equalTo(CHANGED_COLOR_2ND));
    }

    @Test
    @Order(14)
    @DisplayName("activate")
    void changeActiveCherryColor2nd() {

        final Fruit baseFruit = RestAssured.given().when().get("/fruits/" + CHERRY_UUID).then()
                .statusCode(200).extract().jsonPath().getObject("edited.ref", Fruit.class);
        // now Change it with a put on /fruits/{id}
        baseFruit.color = CHANGED_COLOR_2ND;
        // now also set as edited version and use PUT
        baseFruit.activeRevision = true;
        RestAssured.given().with().body(baseFruit).contentType(ContentType.JSON).when()
                .put("/fruits/" + CHERRY_UUID).then().statusCode(200).body("name",
                        CoreMatchers.equalTo(CHERRY_NAME), "color",
                        CoreMatchers.equalTo(CHANGED_COLOR_2ND));
    }

    @Test
    @Order(18)
    void testDeleteSingleFruit() {

        // Delete the Cherry:
        RestAssured.given().when().delete("/fruits/" + CHERRY_UUID).then().statusCode(204);
        RestAssured.given().when().get("/fruits/" + CHERRY_UUID).then().statusCode(404);
        RestAssured.given().when().get("/fruits").then().statusCode(200).body(
                IsNot.not(CoreMatchers.containsString("Cherry")),
                CoreMatchers.containsString("Apple"), CoreMatchers.containsString("Banana"));

    }

    void createNew(Fruit fruit) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(fruit);
        log.info("createNew: {}", json);
        // List all, cherry should be missing now:
        RestAssured.given().with().body(json).contentType(ContentType.JSON).when().post("/fruits")
                .then().statusCode(201).log();
    }

}
