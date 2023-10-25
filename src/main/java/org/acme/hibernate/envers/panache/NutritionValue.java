package org.acme.hibernate.envers.panache;

import java.util.UUID;

import org.acme.hibernate.envers.historized.api.Historizable;
import org.hibernate.envers.Audited;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Audited
@Data
@RequiredArgsConstructor
@NoArgsConstructor
public class NutritionValue extends PanacheEntityBase implements Historizable<UUID> {
    /**
     * the primary key is taken from here. do not rename the field!
     */
    @Id
    @EqualsAndHashCode.Include
    @NonNull
    UUID id;
    @NonNull
    boolean activeRevision;
    Integer editedRevision;
    @NonNull
    String name;
    @NonNull
    String value;
    @JsonbTransient
    @ToString.Exclude
    @ManyToOne
    Fruit fruit;

    @Override
    public void generateId() {
        this.id = UUID.randomUUID();
    }


    public NutritionValue copy() {
        NutritionValue nutritionValue = new NutritionValue();
        nutritionValue.id = this.id;
        nutritionValue.activeRevision = this.activeRevision;
        nutritionValue.editedRevision = editedRevision;
        nutritionValue.name = this.name;
        nutritionValue.value = this.value;
        return nutritionValue;
    }

}
