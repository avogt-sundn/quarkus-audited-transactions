package org.acme.hibernate.envers.historized.impl;

import org.acme.hibernate.envers.panache.Fruit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class BeanMergeTest {
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

    private Fruit getFruit(UUID id, String name, String code) {
        Fruit a = new Fruit();
        a.setId(id);
        a.setName(name);
        a.setColor(code);
        return a;
    }

}