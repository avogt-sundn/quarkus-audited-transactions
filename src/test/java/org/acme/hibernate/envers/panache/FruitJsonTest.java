package org.acme.hibernate.envers.panache;

import java.util.UUID;

import org.acme.global.JacksonSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
/**
 * use non-static fields to hand over from one test to a following.
 * without this annotation each test method receives a new class instance
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
class FruitJsonTest {

    final UUID testFruitUuid = UUID.randomUUID();

    ObjectMapper jacksonMapper = new ObjectMapper();
    static {
        new JacksonSetup().customize(new ObjectMapper());
    }

    @Test
    @Order(1)
    @DisplayName("print the test uuid")
    void writeJson() throws JsonProcessingException {

        Fruit fruit = new Fruit(testFruitUuid, "cherry", "red");
        fruit.addNutritions(new NutritionValue(UUID.randomUUID(), false, "vitamin", "d12"));
        String json = jacksonMapper.writeValueAsString(fruit);

        FruitJsonTest.log.info("serializing Fruit+NutritionValue to this json string {}", json);
        Assertions.assertNotNull(json);
        Assertions.assertTrue(json.contains("cherry"));
        Assertions.assertTrue(json.contains("vitamin"));

    }

    @Test
    @Order(1)
    @DisplayName("print the test uuid")
    void readJson() throws JsonProcessingException {

        final String json = """
                {
                  "id": "3576c18b-b246-4975-b01a-ed9da6da895e",
                  "activeRevision": false,
                  "values": [
                    {
                      "id": "66763a34-66c6-4c9c-8625-97800519ba29",
                      "fruit": "3576c18b-b246-4975-b01a-ed9da6da895e",
                      "activeRevision": false,
                      "name": "vitamin",
                      "value": "d12"
                    }
                  ],
                  "name": "cherry",
                  "color": "red"
                }
                """;
        Fruit fruit = jacksonMapper.readValue(json, Fruit.class);
        Assertions.assertNotNull(fruit);

        FruitJsonTest.log.info(fruit.toString());
        NutritionValue value = fruit.getValues().iterator().next();
        Assertions.assertTrue(value.name.equals("vitamin"));
        Assertions.assertTrue(value.value.equals("d12"));

    }

    @Test
    @Order(2)
    @DisplayName("print the test uuid")
    void readJson_backreferenceMissing() throws JsonProcessingException {

        final String json = """
                {
                  "id": "3576c18b-b246-4975-b01a-ed9da6da895e",
                  "activeRevision": false,
                  "values": [
                    {
                      "id": "66763a34-66c6-4c9c-8625-97800519ba29",
                       "fruit": "3576c18b-b246-4975-b01a-ed9da6da895e",
                      "activeRevision": false,
                      "name": "vitamin",
                      "value": "d12"
                    }
                  ],
                  "name": "cherry",
                  "color": "red"
                }
                """;
        Fruit fruit = jacksonMapper.readValue(json, Fruit.class);
        Assertions.assertNotNull(fruit);

        FruitJsonTest.log.info(fruit.toString());
        NutritionValue value = fruit.getValues().iterator().next();
        Assertions.assertTrue(value.name.equals("vitamin"));
        Assertions.assertTrue(value.value.equals("d12"));

    }


}
