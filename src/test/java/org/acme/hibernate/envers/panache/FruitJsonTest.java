package org.acme.hibernate.envers.panache;

import java.util.UUID;

import org.acme.global.JacksonSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
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

    final static UUID testFruitUuid = UUID.randomUUID();

    static ObjectMapper jacksonMapper = new ObjectMapper();
    static {
        new JacksonSetup().customize(FruitJsonTest.jacksonMapper);
    }

    @Test
    @DisplayName("print the test uuid")
    void writeJsonAndReadBack() throws JsonProcessingException {
        String json;
        UUID randomUUID = UUID.randomUUID();
        {

            Fruit fruit = new Fruit(FruitJsonTest.testFruitUuid, "cherry", "red");

            fruit.addNutritions(new NutritionValue(randomUUID,

                    false, "vitamin", "d12"));
            json = FruitJsonTest.jacksonMapper.writeValueAsString(fruit);

            FruitJsonTest.log.info("serializing Fruit+NutritionValue to this json string {}", json);
            Assertions.assertNotNull(json);
            Assertions.assertTrue(json.contains("cherry"));
            Assertions.assertTrue(json.contains("vitamin"));
        }
        {
            // read back
            Fruit fruit = FruitJsonTest.jacksonMapper.readValue(json, Fruit.class);
            Assertions.assertNotNull(fruit);
            NutritionValue value = fruit.getValues().iterator().next();
            Assertions.assertTrue(value.name.equals("vitamin"));
            Assertions.assertTrue(value.value.equals("d12"));

        }
    }

    @Test
    @DisplayName("print the test uuid")
    void writeManyBackReference() throws JsonProcessingException {
        String json;
        {

            Fruit fruit = new Fruit(FruitJsonTest.testFruitUuid, "cherry", "red");
            fruit.addNutritions(new NutritionValue(UUID.randomUUID(),
                    false, "vitamin", "d12"));
            json = FruitJsonTest.jacksonMapper.writeValueAsString(fruit);

            FruitJsonTest.log.info("serializing Fruit+NutritionValue to this json string {}", json);
            Assertions.assertNotNull(json);
            Assertions.assertTrue(json.contains("cherry"));
            Assertions.assertTrue(json.contains("vitamin"));
        }
        {
            // read back
            Fruit fruit = FruitJsonTest.jacksonMapper.readValue(json, Fruit.class);
            Assertions.assertNotNull(fruit);
            NutritionValue value = fruit.getValues().iterator().next();
            Assertions.assertTrue(value.name.equals("vitamin"));
            Assertions.assertTrue(value.value.equals("d12"));
        }
    }

    @Test
    @DisplayName("print the test uuid")
    void readOneToManyWithBackreference() throws JsonProcessingException {

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
        Fruit fruit = FruitJsonTest.jacksonMapper.readValue(json, Fruit.class);
        Assertions.assertNotNull(fruit);

        FruitJsonTest.log.info(fruit.toString());
        NutritionValue value = fruit.getValues().iterator().next();
        Assertions.assertEquals("vitamin",value.name);
        Assertions.assertEquals("d12",value.value);
        Assertions.assertEquals("3576c18b-b246-4975-b01a-ed9da6da895e", value.fruit.id.toString());
    }

    @Test
    @DisplayName("print the test uuid")
    void readOneToMany_NoBackreference() throws JsonProcessingException {

        final String json = """
                {
                  "id": "3576c18b-b246-4975-b01a-ed9da6da895e",
                  "activeRevision": false,
                  "values": [
                    {
                      "id": "66763a34-66c6-4c9c-8625-97800519ba29",
                      "activeRevision": false,
                      "name": "vitamin",
                      "value": "d12"
                    }
                  ],
                  "name": "cherry",
                  "color": "red"
                }
                """;
        Fruit fruit = FruitJsonTest.jacksonMapper.readValue(json, Fruit.class);
        Assertions.assertNotNull(fruit);

        FruitJsonTest.log.info(fruit.toString());
        NutritionValue value = fruit.getValues().iterator().next();
        Assertions.assertTrue(value.name.equals("vitamin"));
        Assertions.assertTrue(value.value.equals("d12"));

        Assertions.assertEquals(fruit, value.fruit);
    }

}
