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
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.restassured.response.ResponseBodyExtractionOptions;
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
     * methods.
     * no assumptions about the contents of the database are made which makes it
     * robust when
     * running in a suite with other QuarkusTest tests.
     */
    private final static UUID CHERRY_UUID = UUID.randomUUID();
    final static String CHERRY_NAME = "Cherry";
    final static String NO_COLOR = "no";
    final static String CHERRY_COLOR = "red";
    final static String CHANGED_COLOR = "_changed_color";
    final static String CHANGED_COLOR_2ND = "_2nd_changed_color";
    static final String NUTRI_NAME = "_nutri_name";

    @BeforeAll
    static void enableLogging() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @BeforeAll
    static void initAll() {
        RestAssured.defaultParser = Parser.JSON;
        RestAssured.config
                .logConfig((LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails()))
                .objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory((type, s) -> new ObjectMapper()
                        .registerModule(new Jdk8Module())
                        .registerModule(new JavaTimeModule())
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)));
    }

    @Inject
    ObjectMapper objectMapper;

    @Test
    @Order(-1)
    void checkJacksonSetup() {
        Assertions.assertTrue(this.objectMapper.isEnabled(JsonReadFeature.ALLOW_JAVA_COMMENTS.mappedFeature()));
    }

    @Test
    @Order(2)
    void initialDataSet() throws JsonProcessingException {
        Fruit fruit = new Fruit(WriteSingleFruitTest.CHERRY_UUID, true, FruitResourceTest.CHERRY_NAME,
                FruitResourceTest.NO_COLOR);
        fruit.addNutritions(
                new NutritionValue(WriteSingleFruitTest.RANDOM_UUID, true,
                        FruitResourceTest.NUTRI_NAME,
                        WriteSingleFruitTest.NUTRITION_IS_HERE));
        createNew(fruit);

        NutritionValue nutri = (NutritionValue) NutritionValue.findById(WriteSingleFruitTest.RANDOM_UUID);
        Assertions.assertNotNull(nutri);
        Assertions.assertNotNull(nutri.fruit);
        Assertions.assertEquals(WriteSingleFruitTest.CHERRY_UUID, nutri.fruit.id);

        Fruit readFromDB = (Fruit) Fruit.findById(WriteSingleFruitTest.CHERRY_UUID);
        Assertions.assertEquals(1, readFromDB.values.size());
    }

    @Test
    @Order(3)
    void checkInitialDataSet() {

        // List all, should have min. 3 fruits the database has initially:
        final ResponseBodyExtractionOptions response = RestAssured.given()
                .when().get("/fruits/" + WriteSingleFruitTest.CHERRY_UUID)
                .then()
                .statusCode(200)
                .body(
                        CoreMatchers.containsString(WriteSingleFruitTest.NUTRI_NAME),
                        CoreMatchers.containsString(WriteSingleFruitTest.NUTRITION_IS_HERE),
                        CoreMatchers.containsString(WriteSingleFruitTest.CHERRY_NAME))
                // 'find' is gpath special keyword introduncing a filter
                // 'it' is a gpath special keyword referencing the current node
                // 'name' and 'id' are the field names from the fruit class that got serialized
                // to json
                .extract().body();
        String oneValidId = response.jsonPath()
                .getString("findAll {it -> it.name == 'Cherry' }.id");

        Assertions.assertEquals(WriteSingleFruitTest.CHERRY_UUID.toString(), oneValidId);
    }

    void createNew(Fruit fruit) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(fruit);
        WriteSingleFruitTest.log.info("createNew: {}", json);
        // List all, cherry should be missing now:
        RestAssured.given().with().body(json).contentType(ContentType.JSON)
                .when().post("/fruits")
                .then()
                .statusCode(201).log();
    }

}
