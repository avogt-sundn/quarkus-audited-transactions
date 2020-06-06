package org.acme.hibernate.orm.envers;


import lombok.extern.slf4j.Slf4j;
import org.acme.hibernate.orm.historized.Historized;
import org.acme.hibernate.orm.panache.Fruit;
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
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class HistorizedRepository<T> {

    private final Class<T> clz;
    private final AuditReader reader;

    public HistorizedRepository(Class<T> clz, EntityManager entityManager) {
        this.reader = AuditReaderFactory.get(entityManager);
        this.clz = clz;
    }

    public HistoryList<T> getList(@PathParam("id") UUID id) {
        List<Number> revisions = reader.getRevisions(Fruit.class, id);
        List<History<T>> collect = revisions.stream().map(
                rev -> new History<T>(
                        reader.find((Class<T>) clz, (Object) id, rev),
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
    public Optional<Historized<T>> getSingle(UUID id) {
        log.info("getSingle(id: " + id + ")");

        
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
    private Optional<History<T>> loadHistory(UUID id, boolean active, Number mustBeHigherThan) {
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

    private Optional<History<T>> readQuery(UUID id, AuditQuery auditQuery) {

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

    private Optional<History<T>> readProjection(UUID id, AuditQuery auditQuery) {
        // read the entity itself now
        Number found = null;
        try {
            Object singleResult = auditQuery.getSingleResult();
            log.debug("singleResult={}", singleResult);
            found = (Number) singleResult;
        } catch (NoResultException e) {
            found = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        // need a final for the closures
        final Number searchForRevisionNumber = found;
        return Optional.ofNullable(searchForRevisionNumber)
                .map(r -> reader.find(this.clz, id, searchForRevisionNumber))
                .<History<T>>map(f -> new History<T>(f, searchForRevisionNumber,
                        reader.findRevision(CustomRevisionEntity.class, searchForRevisionNumber)));
    }

}
