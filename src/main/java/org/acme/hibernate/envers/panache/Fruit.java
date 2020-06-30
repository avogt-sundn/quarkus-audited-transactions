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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@EqualsAndHashCode(callSuper = false)
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
    boolean activeRevision;
    Integer editedRevision;

    @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER)
    Set<NutritionValue> values;

    @Column(length = 40, unique = true)
    @javax.validation.constraints.NotNull
    String name;
    @javax.validation.constraints.NotNull
    String color;

    public Fruit(UUID uuid, boolean b, String name, String color) {
        this.id = uuid;
        this.activeRevision = b;
        this.name = name;
        this.color = color;
    }

    Fruit copy() {
        Fruit fruit = new Fruit();
        fruit.id = id;
        fruit.name = name;
        fruit.color = color;
        // if there is a set, copy values into a new set and set reference to the new fruit
        fruit.values = Optional.ofNullable(values).stream().flatMap(set -> (Stream<NutritionValue>) set.stream())
                .map(n -> n.copy()).collect(Collectors.toSet());
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

    }
}
