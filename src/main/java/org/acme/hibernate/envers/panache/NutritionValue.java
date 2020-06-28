package org.acme.hibernate.envers.panache;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.acme.hibernate.envers.historized.api.Historizable;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Id;
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
    @EqualsAndHashCode.Exclude
    boolean activeRevision;
    @EqualsAndHashCode.Exclude
    Integer editedRevision;
    @NonNull
    @EqualsAndHashCode.Exclude
    String name;
    @NonNull
    @EqualsAndHashCode.Exclude
    String value;


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
