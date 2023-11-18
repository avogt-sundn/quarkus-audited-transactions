package org.acme.hibernate.envers.panache;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
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
 * use non-static fields to hand over from one test to a following. without this
 * annotation each test method receives a new class instance
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FruitJpaTest {

    final UUID testFruitUuid = java.util.UUID.randomUUID();
    private int lastNumberOfValuesSeen;

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
        Fruit fruit = new Fruit(testFruitUuid, "jpa_fruit", "red");
        fruit.addNutritions(new NutritionValue(java.util.UUID.randomUUID(), false, "number", "0"));
        Assertions.assertEquals(1, fruit.getValues().size());
        fruit.persist();
    }

    @Test
    @Order(6)
    @DisplayName("check creation - set mappedBy")
    void checkCreate() {
        final Fruit fruit = Fruit.findById(testFruitUuid);
        Assertions.assertNotNull(fruit, "got persisted");
        final int size = fruit.getValues().size();
        Assertions.assertEquals(1, size);
    }

    @Test
    @Order(7)
    @DisplayName("add to the set")
    void addNutrition() {

        Fruit fruit = Fruit.findById(testFruitUuid);
        fruit.addNutritions(new NutritionValue("number", "1"));
        fruit.persist();
        Assertions.assertNotNull(fruit.getValues().stream().allMatch(v -> v.fruit.id != null),
                "backreference is filled");
        this.lastNumberOfValuesSeen = fruit.values.size();
    }

    @Test
    @Order(8)
    @DisplayName("add to the set")
    void checkNutrition() {
        // clear
        Fruit.getEntityManager().clear();
        Fruit fruit = Fruit.findById(testFruitUuid);
        Assertions.assertNotNull(fruit);
        Assertions.assertEquals(fruit.id, fruit.getValues().iterator().next().fruit.id);
        Assertions.assertEquals(lastNumberOfValuesSeen, fruit.values.size());
    }

    @Test
    @Order(9)
    @DisplayName("delete from the set")
    void removeFromSet() {

        final Fruit fruit = Fruit.findById(testFruitUuid);
        NutritionValue[] nutritionValues = fruit.getValues().toArray(new NutritionValue[1]);
        Assertions.assertEquals(2, nutritionValues.length, "expect to have 2 values");

        fruit.removeNutrition(nutritionValues[0]);

        fruit.persist();

        nutritionValues[0].delete();
        this.lastNumberOfValuesSeen = fruit.values.size();
    }

    @Test
    @Order(10)
    @DisplayName("check delete from the set")
    void checkRemoveFromSet() {
        Panache.getEntityManager().clear();
        final Fruit fruit = Fruit.findById(testFruitUuid);
        final int size = fruit.getValues().size();
        Assertions.assertEquals(lastNumberOfValuesSeen, size);
    }

    @Test
    @Order(11)
    @DisplayName("check entitymanager.clear works")
    void entityManagerClearWorks() {
        final Fruit test = Fruit.findById(testFruitUuid);
        // erase entity beans previously read:
        Panache.getEntityManager().clear();
        final Fruit fruit = Fruit.findById(testFruitUuid);
        Assertions.assertNotSame(test, fruit);
    }

    @Test
    @Order(13)
    @DisplayName("delete root")
    void deleteRoot() {
        final Fruit fruit = Fruit.findById(testFruitUuid);
        fruit.delete();
    }

    @Test
    @Order(14)
    @DisplayName("check delete root")
    void checkDeleteRoot() {
        Panache.getEntityManager().clear();
        final Fruit fruit = Fruit.findById(testFruitUuid);
        Assertions.assertNull(fruit);
    }

}
