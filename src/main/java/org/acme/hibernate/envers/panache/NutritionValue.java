package org.acme.hibernate.envers.panache;

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
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.util.UUID;

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
