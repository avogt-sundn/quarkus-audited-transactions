package org.acme.hibernate.envers.panache;

import static org.hamcrest.Matchers.*;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
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
class WriteSingleFruitTest {

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
    static final String NUTRI_NAME = "_nutri_name";

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
        fruit.addNutritions(new NutritionValue(RANDOM_UUID, true, FruitResourceTest.NUTRI_NAME,
                NUTRITION_IS_HERE));
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

    void createNew(Fruit fruit) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(fruit);
        log.info("createNew: {}", json);
        // List all, cherry should be missing now:
        RestAssured.given().with().body(json).contentType(ContentType.JSON).when().post("/fruits")
                .then().statusCode(201).log();
    }

}
