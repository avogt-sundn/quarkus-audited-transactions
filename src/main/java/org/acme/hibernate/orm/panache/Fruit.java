package org.acme.hibernate.orm.panache;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
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
@NoArgsConstructor
@Audited
@RegisterForReflection
public class Fruit extends PanacheEntityBase {

    /**
     * the primary key is taken from here. do not rename the field!
     */
    @Id
    public UUID uuid;

    @Version
    public int version;

    @Column(length = 40, unique = true)
    @javax.validation.constraints.NotNull
    public String name;
    public String color;

    public Fruit(String name) {
        this.name = name;
    }

    public Fruit(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public Fruit copy() {
        Fruit fruit = new Fruit();
        fruit.uuid = uuid;
        fruit.version = version;
        fruit.name = name;
        fruit.color = color;
        return fruit;
    }
}
