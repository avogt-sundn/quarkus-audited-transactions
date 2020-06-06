package org.acme.hibernate.orm.envers;


import lombok.extern.slf4j.Slf4j;
import org.acme.hibernate.orm.historized.Historized;
import org.acme.hibernate.orm.panache.Fruit;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import javax.persistence.EntityManager;
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
     *
     * @param id - primary key
     * @return - Optional with Historized wrapper containing the entity to the given id.
     * - Optional with value null if the id is not present.
     */
    public Optional<Historized<T>> getSingle(UUID id) {
        log.info("getSingle(id: " + id + ")");

        AuditQuery auditQuery = reader.createQuery()
                .forRevisionsOfEntity(this.clz, true, false)
                .addProjection(AuditEntity.revisionNumber().max())
                .add(AuditEntity.id().eq(id))
                .add(AuditEntity.property("active").eq(Boolean.TRUE));
        // returns null if no entity was found
        Number found = (Number) auditQuery.getSingleResult();
        Optional<Number> revision = Optional.ofNullable(found);
        Optional<Historized<T>> result = revision
                .map(r -> reader.find(this.clz, id, found))
                .map(f -> new Historized(new History(f)));
        return result;
    }
}
