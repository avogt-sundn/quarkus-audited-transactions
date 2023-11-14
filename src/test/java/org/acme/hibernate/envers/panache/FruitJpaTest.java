package org.acme.hibernate.envers.panache;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@QuarkusTest
@Transactional
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
/**
 * use non-static fields to hand over from one test to a following.
 * without this annotation each test method receives a new class instance
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FruitJpaTest {

    final UUID testFruitUuid = java.util.UUID.randomUUID();

    @Test
    @Order(1)
    @DisplayName("print the test uuid")
    void printTestUuid() {
        System.out.println(testFruitUuid);
        Assertions.assertNotNull(testFruitUuid);
    }

    @Test
    @Order(5)
    @DisplayName("add to the set")
    void createOne() {
        Fruit fruit = new Fruit(testFruitUuid, "cherry", "red");
        fruit.addNutritions(new NutritionValue(java.util.UUID.randomUUID(), false, "name", "value"));
        Assertions.assertEquals(1,
                fruit.getValues().size());
        FruitJpaTest.log.debug("to database: {}", fruit);
        fruit.persist();
    }

    @Test
    @Order(5)
    @DisplayName("add to the set")
    void createWithSet_NoBackReference() {

        Fruit fruit = new Fruit("cherry", "red");
        fruit.addNutritions(new NutritionValue("color", "name"));
        
        Assertions.assertEquals(1,
                fruit.getValues().size());
        FruitJpaTest.log.debug("to database: {}", fruit);
        fruit.persist();
        Assertions.assertNotNull(fruit.id);
        Assertions.assertNotNull(fruit.getValues().iterator().next().fruit.id);

    }

    @Test
    @Order(6)
    @DisplayName("check creation - set mappedBy")
    void checkCreate() {
        final Fruit fruit = Fruit.findById(testFruitUuid);
        FruitJpaTest.log.debug("from database: {}", fruit);
        Assertions.assertNotNull(fruit, "");
        final int size = fruit.getValues().size();
        Assertions.assertEquals(1, size);
    }

    @Test
    @Disabled
    @Order(8)
    @DisplayName("delete from the set")
    void removeOne() {
        final Fruit fruit = Fruit.findById(testFruitUuid);
        FruitJpaTest.log.debug("from database: {}", fruit);
        Assertions.assertEquals(1, fruit.getValues().size());
        NutritionValue[] nutritionValues = fruit.getValues().toArray(new NutritionValue[1]);
        Arrays.stream(nutritionValues).forEach(nutri -> fruit.getValues().remove(nutri));
        fruit.persist();
        Assertions.assertEquals(0, fruit.getValues().size());
    }

    @Test
    @Order(9)
    @DisplayName("check deletion in the set")
    void checkDelete() {
        final Fruit fruit = Fruit.findById(testFruitUuid);
        FruitJpaTest.log.debug("from database: {}", fruit);
        final int size = fruit.getValues().size();
        if (size == 1) {
            fruit.getValues().toArray(new NutritionValue[1])[0].setFruit(null);
            Panache.getEntityManager().merge(fruit);
        }
        Assertions.assertEquals(0, size);
    }

    @Test
    @Order(10)
    @DisplayName("check deletion via mappedBy")
    void checkDelete2() {
        final Fruit fruit = Fruit.findById(testFruitUuid);
        Assertions.assertNotNull(fruit);
        FruitJpaTest.log.debug("from database: {}", fruit);
        final int size = fruit.getValues().size();
        Assertions.assertEquals(0, size);
    }

    @Test
    @Order(20)
    @DisplayName("add 2nd to the collection")
    void createsecond() {
        Fruit fruit = new Fruit(testFruitUuid, "cherry", "red");
        fruit.addNutritions(new NutritionValue(java.util.UUID.randomUUID().randomUUID(), false, "name", "value"));
        FruitJpaTest.log.debug("from database: {}", fruit);
        Panache.getEntityManager().merge(fruit);
    }
}
