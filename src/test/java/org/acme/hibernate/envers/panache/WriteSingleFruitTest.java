package org.acme.hibernate.envers.panache;

import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Test the /fruits/ resource with GET,POST,PUT,DELETE
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
class WriteSingleFruitTest {

    static final String NUTRITION_IS_HERE = "nutrition-is-here";
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

    final UUID testFruitUuid = UUID.randomUUID();

    @Inject
    ObjectMapper jacksonMapper;

    @BeforeAll
    static void enableLogging() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }


    @Test
    @Order(2)
    void initialDataSet() throws JsonProcessingException {
        Fruit fruit = new Fruit(FruitResourceTest.CHERRY_UUID, true, FruitResourceTest.CHERRY_NAME, FruitResourceTest.NO_COLOR);
        fruit.addNutritions(new NutritionValue(UUID.randomUUID(), true, FruitResourceTest.NUTRI_NAME, WriteSingleFruitTest.NUTRITION_IS_HERE));
        createNew(fruit);
        createNew(new Fruit(UUID.randomUUID(), true, "Apple", "green"));
        createNew(new Fruit(UUID.randomUUID(), true, "Banana", "yellow"));

    }

    @Test
    @Order(3)
    void checkInitialDataSet() {

        //List all, should have min. 3 fruits the database has initially:
        final String oneValidId = RestAssured.given()
                .when().get("/fruits")
                .then()
                .statusCode(200)
                .body(
                        CoreMatchers.containsString(WriteSingleFruitTest.NUTRI_NAME),
                    CoreMatchers.containsString(WriteSingleFruitTest.NUTRITION_IS_HERE),
                        CoreMatchers.containsString(WriteSingleFruitTest.CHERRY_NAME),
                        CoreMatchers.containsString("Apple"),
                        CoreMatchers.containsString("Banana")
                )
                // 'find' is gpath special keyword introduncing a filter
                // 'it' is a gpath special keyword referencing the current node
                // 'name' and 'id' are the field names from the fruit class that got serialized to json
                .extract().body().jsonPath().getString("find{ it.name == '" + FruitResourceTest.CHERRY_NAME + "' }.id");

        Assertions.assertEquals(FruitResourceTest.CHERRY_UUID.toString(), oneValidId);
    }

    void createNew(Fruit fruit) throws JsonProcessingException {
        String json = jacksonMapper.writeValueAsString(fruit);
        //List all, cherry should be missing now:
        RestAssured.given().with().body(json).contentType(ContentType.JSON)
                .when().post("/fruits")
                .then()
                .statusCode(201)
                .extract().body().as(Fruit.class).getName().equals(fruit.name);
    }


}
