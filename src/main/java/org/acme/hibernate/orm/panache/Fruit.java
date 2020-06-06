package org.acme.hibernate.orm.panache;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.acme.hibernate.orm.historized.Historizable;
import org.hibernate.envers.Audited;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Entity
@Cacheable
@Data
@RequiredArgsConstructor
@NoArgsConstructor
@Audited
@RegisterForReflection
public class Fruit extends PanacheEntityBase implements Historizable {

    /**
     * the primary key is taken from here. do not rename the field!
     */
    @Id
    @NonNull
    public UUID id;
    /**
     * only active entities are fetched by queries, representing a current state at one point in time.
     * if active is false, this entity is an edited version.
     */
    @NonNull
    boolean active;

    @Version
    public int version;

    @Column(length = 40, unique = true)
    @javax.validation.constraints.NotNull
    @NonNull
    public String name;
    @NonNull
    public String color;

    public Fruit copy() {
        Fruit fruit = new Fruit();
        fruit.id = id;
        fruit.version = version;
        fruit.name = name;
        fruit.color = color;
        return fruit;
    }
}
