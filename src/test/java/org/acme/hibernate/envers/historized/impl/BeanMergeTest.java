package org.acme.hibernate.envers.historized.impl;

import java.util.UUID;

import org.acme.hibernate.envers.panache.Fruit;
import org.acme.hibernate.envers.panache.NutritionValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BeanMergeTest {
    @Test
    @DisplayName("overlay wins")
    void a() {

        Fruit a = getFruit(UUID.randomUUID(), "a", "a");
        Fruit b = getFruit(UUID.randomUUID(), "b", "b");


        Fruit c = BeanMerge.merge2On1(a, b);
        Assertions.assertEquals(c.getColor(), b.getColor());
        Assertions.assertEquals(c.getName(), b.getName());
        Assertions.assertEquals("a", a.getName());
        Assertions.assertEquals("a", a.getName());

    }

    @Test
    @DisplayName("null must not overwrite non-null in the base")
    void b() {

        Fruit a = getFruit(UUID.randomUUID(), "a", "a");
        Fruit b = getFruit(UUID.randomUUID(), null, "b");

        Fruit c = BeanMerge.merge2On1(a, b);
        Assertions.assertEquals(c.getName(), a.getName());
        Assertions.assertEquals(c.getColor(), b.getColor());
        Assertions.assertEquals("a", a.getName());
        Assertions.assertEquals("a", a.getName());

    }


    @Test
    @DisplayName("null must not overwrite non-null in the base")
    void c() {
        final UUID fruitId = UUID.randomUUID();
        final UUID nutriId = UUID.randomUUID();

        Fruit a = getFruit(fruitId, "a", "a");
        a.addNutritions(new NutritionValue(nutriId, false, "a.n", "val.a.n"));
        Fruit b = getFruit(fruitId, "a", "b");
        final String changed = "a.changed";
        b.addNutritions(new NutritionValue(nutriId, false, "a.new", changed));

        Fruit c = BeanMerge.merge2On1(a, b);
        Assertions.assertEquals(1, c.getValues().stream().filter(n -> n.getId().equals(nutriId))
                .filter(n -> n.getValue().equals(changed)).count());

    }

    private Fruit getFruit(UUID id, String name, String color) {
        Fruit a = new Fruit(id, false, name, color);
        a.setId(id);
        a.setName(name);
        a.setColor(color);
        return a;
    }

}