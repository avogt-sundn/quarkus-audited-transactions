
## Hibernate Envers

The Fruit entity bean class is marked with ``@Audited``.
This will result in a history of all changes being conducted.

    package org.acme.hibernate.orm.panache;
    
    import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
    import org.hibernate.envers.Audited;
    
    @Audited
    @Entity
    @RegisterForReflection
    public class Fruit extends PanacheEntityBase {
        ...
    }
    
 
    
