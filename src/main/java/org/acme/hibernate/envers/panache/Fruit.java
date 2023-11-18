package org.acme.hibernate.envers.panache;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.acme.hibernate.envers.historized.api.Historizable;
import org.hibernate.envers.Audited;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Cacheable
@Data
@ToString
// @RequiredArgsConstructor
@NoArgsConstructor
@Audited
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Fruit extends PanacheEntityBase implements Historizable<UUID> {

    /**
     * the primary key is taken from here. do not rename the field!
     */
    @Id
    @EqualsAndHashCode.Include
    // @NonNull
    @Column(unique = true)
    UUID id;
    /**
     * only active entities are fetched by queries, representing a current state at
     * one point in time. if active is false, this entity is an edited version.
     */
    boolean activeRevision;
    Integer editedRevision;

    @OneToMany(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER, mappedBy = "fruit")
    @JsonDeserialize
    Set<NutritionValue> values;

    @Column(length = 40, unique = false)
    // @NotNull
    String name;
    // @NotNull
    String color;

    public Fruit(UUID uuid, boolean isActiveRevision, String name, String color) {
        this.id = uuid;
        this.activeRevision = isActiveRevision;
        this.name = name;
        this.color = color;
    }

    public Fruit(UUID uuid, String name, String color) {
        this(uuid, false, name, color);
    }

    Fruit copy() {
        final Fruit fruit = new Fruit();
        fruit.id = id;
        fruit.name = name;
        fruit.color = color;
        // if there is a set, copy values into a new set and set reference to the new
        // fruit
        fruit.values = Optional.ofNullable(values).stream()
                .flatMap(set -> (Stream<NutritionValue>) set.stream()).map(n -> n.copy()).map(n -> {
                    n.fruit = fruit;
                    return n;
                }).collect(Collectors.toSet());
        return fruit;
    }

    @Override
    public void generateId() {
        this.id = UUID.randomUUID();
    }

    /**
     * called during deserialiation, this setter allows to fix the back references
     * from Nutrition to the parent Fruit.
     *
     * @param set
     */
    public void setValues(Set<NutritionValue> set) {
        this.values = set;
        if (this.id != null && this.values != null) {
            Fruit parent = this;
            this.values.stream().filter(v -> v.fruit == null).forEach(v -> v.fruit = parent);
        }
    }

    public void addNutritions(NutritionValue... val) {
        if (null == this.values) {
            this.values = new HashSet<>();
        }
        if (this.getId() == null) {
            this.generateId();
        }
        List<NutritionValue> nutritionValues = Arrays.asList(val);
        nutritionValues.stream().filter(n -> n.id == null).forEach(n -> n.setId(UUID.randomUUID()));
        this.values.addAll(nutritionValues);
        nutritionValues.forEach(nv -> nv.fruit = this);
    }

    public Fruit(@NonNull String name, String color) {
        this.name = name;
        this.color = color;
    }

    public void removeNutrition(NutritionValue nutritionValue) {
        if (this.values != null && nutritionValue != null) {
            this.values.removeIf(v -> v.equals(nutritionValue));
            // do not forget to clear the backreference when you want that association be
            // gone!
            nutritionValue.fruit = null;
        }
    }

}
