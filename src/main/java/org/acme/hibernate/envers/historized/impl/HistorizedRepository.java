package org.acme.hibernate.envers.historized.impl;


import lombok.extern.slf4j.Slf4j;
import org.acme.hibernate.envers.historized.api.Historizable;
import org.acme.hibernate.envers.historized.api.Historized;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class HistorizedRepository<T extends Historizable<I>, I> {

    private final Class<T> clz;
    private final AuditReader reader;
    private final EntityManager entityManager;

    public HistorizedRepository(Class<T> clz, EntityManager entityManager) {
        this.reader = AuditReaderFactory.get(entityManager);
        this.clz = clz;
        this.entityManager = entityManager;
    }

    public HistoryList<T> getList(@PathParam("id") I id) {
        List<Number> revisions = reader.getRevisions(this.clz, id);
        List<History<T>> collect = revisions.stream().map(
                rev -> new History<T>(
                        reader.find((Class<T>) this.clz, (Object) id, rev),
                        rev,
                        reader.findRevision(CustomRevisionEntity.class, rev)
                )
        ).collect(Collectors.toList());
        return new HistoryList<T>(collect);
    }

    /**
     * Fetch single entity to a given id.
     * The result contains the newest active version and the newest edited version if they exist.
     * Both can be null independent from each other.
     *
     * @param id - primary key
     * @return - Optional with Historized wrapper containing the entity to the given id.
     * - Optional with value null if the id is not present.
     */
    public Optional<Historized<T>> getSingle(I id) {

        // history will never be deleted therefore we must do a find query to see if the entity currently still exists
        if (entityManager.find(clz, id) == null) {
            return Optional.empty();
        }

        // both active and edited entity can be null independently, so we have to load either way:
        Optional<History<T>> activeHistory = loadHistory(id, true, null);
        // if we have an active version, only consider the edited versions after theirs revision
        Optional<History<T>> editedHistory = loadHistory(id, false,
                activeHistory.map(h -> h.revision).orElse(null));

        if (activeHistory.orElse(editedHistory.orElse(null)) != null) {

            return Optional.of(new Historized<T>(
                    activeHistory.orElse(null),
                    editedHistory.orElse(null),
                    null));
        }
        return Optional.ofNullable(null);
    }

    /**
     * fetch to the id, latest revision, active status
     *
     * @param id
     * @param active
     * @param mustBeHigherThan
     * @return
     */
    private Optional<History<T>> loadHistory(I id, boolean active, Number mustBeHigherThan) {
        // as soon as you use a projection the result will be the revision number
        // we wont do a projection in order to get also the deleted entries
        AuditQuery auditQuery = reader.createQuery()
                .forRevisionsOfEntity(this.clz, false, true)
                .add(AuditEntity.id().eq(id))
                .add(AuditEntity.property("active").eq(active))
                // the use of this projection leads the getSingleResult to return a Number containing the revision
                //.addProjection(AuditEntity.revisionNumber().max())
                // get the highest revision number available by sorting descending plus limiting to 1 entity result
                .addOrder(AuditEntity.revisionNumber().desc())
                .setMaxResults(1);

        if (mustBeHigherThan != null) {
            auditQuery.add(AuditEntity.revisionNumber().ge(mustBeHigherThan));
        }
        return readQuery(id, auditQuery);

    }

    private Optional<History<T>> readQuery(I id, AuditQuery auditQuery) {

        Object[] result = null;
        try {
            Object singleResult = auditQuery.getSingleResult();
            log.debug("singleResult={}", singleResult);
            result = (Object[]) singleResult;
        } catch (NoResultException e) {
            result = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        // need a final for the closures

        return Optional.ofNullable(result).filter(ft -> !((RevisionType) ft[2]).equals(RevisionType.DEL))
                .map(rs -> {
                    final T t = (T) rs[0];
                    final CustomRevisionEntity c = (CustomRevisionEntity) rs[1];
                    final RevisionType rt = (RevisionType) rs[2];
                    return new History<T>(t, c.getId(), c);
                });
    }

    public void persist(T t) {
        entityManager.persist(t);
    }

    public T merge(T t) {
        return entityManager.merge(t);
    }

    public <I> T partialUpdate(I id, T t) {
        // FIXME: against which version do we want to merge? active or edited?
        final T fromDatabase = entityManager.find(clz, id);
        BeanMerge.merge(fromDatabase, t);
        // merge will replace all stored values with the ones received - null will overwrite!
        T merged = entityManager.merge(fromDatabase);
        return merged;
    }

    public <I> void delete(I id) {
        entityManager.remove(entityManager.find(clz, id));
    }
}
