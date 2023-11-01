package org.acme.hibernate.envers.panache;

import java.util.UUID;

import org.acme.hibernate.envers.historized.api.Historizable;
import org.hibernate.envers.Audited;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
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

@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")
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

    @ToString.Exclude
    @ManyToOne
    @JsonBackReference
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
