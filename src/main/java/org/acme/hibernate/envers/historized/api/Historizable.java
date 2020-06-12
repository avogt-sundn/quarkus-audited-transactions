package org.acme.hibernate.envers.historized.api;

/**
 * Express requirements for any entity bean that wants to be treated in a historizing manner.
 * <p>
 * Preferred id type for rest entities is a UUID:
 * - UUIDs are not database dependent (imagine switching from SQL to non sql in your implementation)
 * - the client can generate a unique UUID on its own which spares the roundtrip to the service.
 * - UUIDs can also be imported and exported with dramatically reduced likelyhood of clashes.
 * <p>
 * The {@link org.acme.hibernate.envers.historized.impl.HistorizedRepository} will call find(I id).
 * It must therefore be the @Id annotated field in your entity class.
 *
 * @param <I> the Java type used for the @Id field in your @Entity bean
 */
public interface Historizable<I> {
    I getId();

    void setId(I id);

    /**
     * if the rest service received a bean via POST that has no id set,
     * it will call this method to create and set a new id.
     */
    void generateId();

    boolean isActiveRevision();

    Integer getEditedRevision();

    void setEditedRevision(Integer rev);

}
