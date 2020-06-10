package org.acme.hibernate.envers.panache;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.acme.hibernate.envers.historized.api.Historizable;
import org.hibernate.envers.Audited;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.util.UUID;

@Entity
@EqualsAndHashCode(callSuper = false)
@Audited
@Data
@RequiredArgsConstructor
@NoArgsConstructor
public class NutritionValue extends PanacheEntityBase implements Historizable<UUID> {
    /**
     * the primary key is taken from here. do not rename the field!
     */
    @Id
    @NonNull
    UUID id;
    @NonNull
    boolean active;
    @NonNull
    String name;
    @NonNull
    String value;

    // stop infinite loop on json and toString and equals/hashCode:
    @JsonIgnore
    @JsonbTransient
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    Fruit fruits;

    @Override
    public void generateId() {
        this.id = UUID.randomUUID();
    }

    public NutritionValue copy() {
        NutritionValue nutritionValue = new NutritionValue();
        nutritionValue.id = this.id;
        nutritionValue.active = this.active;
        nutritionValue.name = this.name;
        nutritionValue.value = this.value;
        return nutritionValue;
    }

    public Fruit getFruits() {
        return fruits;
    }

    public void setFruits(Fruit fruits) {
        this.fruits = fruits;
    }
}
