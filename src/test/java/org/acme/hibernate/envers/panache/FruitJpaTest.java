package org.acme.hibernate.envers.panache;

import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@Transactional
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
/**
 * use non-static fields to hand over from one test to a following.
 * without this annotation each test method receives a new class instance
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FruitJpaTest {

    public final UUID UUID = java.util.UUID.randomUUID();

    @Test
    @Order(5)
    @DisplayName("add to the collection")
    public void createOne() {
        Fruit fruit = new Fruit(UUID, "cherry", "red");
        fruit.addNutritions(new NutritionValue(java.util.UUID.randomUUID().randomUUID(), false, "name", "value"));
        log.debug("from database: {}", fruit);
        fruit.persist();
    }

    @Test
    @Order(6)
    @DisplayName("delete from the collection")
    public void removeOne() {
        final Fruit fruit = Fruit.findById(UUID);
        log.debug("from database: {}", fruit);
        assertEquals(1, fruit.getValues().size());
        NutritionValue[] nutritionValues = fruit.getValues().toArray(new NutritionValue[1]);
        Arrays.stream(nutritionValues).forEach(nutri -> fruit.getValues().remove(nutri));
        fruit.persist();
    }

    @Test
    @Order(7)
    @DisplayName("check deletion")
    public void checkDelete() {
        final Fruit fruit = Fruit.findById(UUID);
        log.debug("from database: {}", fruit);
        assertEquals(0, fruit.getValues().size());
    }
}
