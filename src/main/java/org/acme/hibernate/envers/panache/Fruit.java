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

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Cacheable
@Data
@ToString
@RequiredArgsConstructor
@NoArgsConstructor
@Audited
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Fruit extends PanacheEntityBase implements Historizable<UUID> {

    /**
     * the primary key is taken from here. do not rename the field!
     */
    @Id
    @EqualsAndHashCode.Include
    @NonNull
    @Column( unique = true)
    UUID id;
    /**
     * only active entities are fetched by queries, representing a current state at
     * one point in time.
     * if active is false, this entity is an edited version.
     */
    boolean activeRevision;
    Integer editedRevision;

    @OneToMany(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER, mappedBy = "fruit")
    Set<NutritionValue> values;

    @Column(length = 40, unique = false)
    @NotNull
    String name;
    @NotNull
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
        Fruit fruit = new Fruit();
        fruit.id = id;
        fruit.name = name;
        fruit.color = color;
        // if there is a set, copy values into a new set and set reference to the new
        // fruit
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
        if (this.getId() == null) {
            this.generateId();
        }
        List<NutritionValue> nutritionValues = Arrays.asList(val);
        nutritionValues.stream().filter(n -> n.id==null).forEach(n -> n.setId(UUID.randomUUID()));
        this.values.addAll(nutritionValues);
        nutritionValues.forEach(nv -> {
            nv.fruit = this;
        });
    }

    public Fruit(String name, String color) {
        this.name = name;
        this.color = color;
    }

}
