package org.acme.hibernate.envers.panache;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.acme.hibernate.envers.historized.api.Historizable;
import org.hibernate.envers.Audited;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Entity
@Cacheable
@Data
@RequiredArgsConstructor
@NoArgsConstructor
@Audited
public class Fruit extends PanacheEntityBase implements Historizable<UUID> {

    /**
     * the primary key is taken from here. do not rename the field!
     */
    @Id
    @NonNull
    UUID id;
    /**
     * only active entities are fetched by queries, representing a current state at one point in time.
     * if active is false, this entity is an edited version.
     */
    @NonNull
    boolean active;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    Set<NutritionValue> values;

    @Column(length = 40, unique = true)
//    @javax.validation.constraints.NotNull
    @NonNull
    String name;
    @NonNull
    String color;

    Fruit copy() {
        Fruit fruit = new Fruit();
        fruit.id = id;
        fruit.name = name;
        fruit.color = color;
        return fruit;
    }

    @Override
    public void generateId() {
        this.id = UUID.randomUUID();
    }

    public void addNutritions(NutritionValue... val) {
        if (null == this.values) {
            this.values = new HashSet<NutritionValue>();
        }
        List<NutritionValue> nutritionValues = Arrays.asList(val);
        this.values.addAll(nutritionValues);
        nutritionValues.stream().forEach(v -> v.setFruit(this));
    }
}
