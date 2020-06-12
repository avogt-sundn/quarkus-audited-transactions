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

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

    private Transactor<T> createBean() {
        try {
            final BeanManager beanManager = CDI.current().getBeanManager();
            final Set<Bean<?>> bearerService = beanManager.getBeans(Transactor.TRANSACTOR);
            final Bean<?> bean = bearerService.toArray(new Bean<?>[0])[0];
            CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
            final Transactor identityService = (Transactor) beanManager.getReference(bean, Transactor.class, ctx);

            return identityService;

        } catch (Exception e) {
            log.error("failed to get cdi bean: " + IdentityService.class.getName());
            throw e;
        }
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

        // CustomRevisionEntity of the current version always know if there is an edited version
        Optional<History<T>> optionalHistory = loadCurrent(id);

        return optionalHistory.map(hy ->
                // we have a revision available, lets produce a Historized summary
                new Historized<T>(
                        getActive(hy),
                        getEdited(hy), null
                ));
    }

    private History<T> getActive(History<T> hy) {
        // only if there has been no active revision yet assigned will we not produce any active history
        if (hy.ref.isActiveRevision()) {
            return hy;
        } else {
            return null;
        }
    }

    private History<T> getEdited(History<T> hy) {
        if (null != hy && null != hy.ref && null != hy.ref.getEditedRevision()) {
            return loadRevision(hy.ref.getId(), hy.ref.getEditedRevision()).orElse(null);
        } else {
            return null;
        }
    }

    /**
     * fetch newest/latest revision to the id
     *
     * @param id
     * @return
     */
    private Optional<History<T>> loadCurrent(I id) {
        // as soon as you use a projection the result will be the revision number
        // we wont do a projection in order to get also the deleted entries
        AuditQuery auditQuery = reader.createQuery()
                .forRevisionsOfEntity(this.clz, false, true)
                .add(AuditEntity.id().eq(id))
                // get the highest revision number available by sorting descending plus limiting to 1 entity result
                .addOrder(AuditEntity.revisionNumber().desc())
                .setMaxResults(1);

        return readQuery(id, auditQuery);

    }

    /**
     * fetch newest/latest revision to the id
     *
     * @param id
     * @return
     */
    private Optional<History<T>> loadRevision(I id, Number revision) {
        AuditQuery auditQuery = reader.createQuery()
                .forRevisionsOfEntity(this.clz, false, true)
                .add(AuditEntity.revisionNumber().eq(revision))
                .add(AuditEntity.id().eq(id))
                .setMaxResults(1);

        return readQuery(id, auditQuery);

    }

    private Optional<History<T>> readQuery(I id, AuditQuery auditQuery) {

        Object[] result = null;
        try {
            Object singleResult = auditQuery.getSingleResult();
            result = (Object[]) singleResult;
        } catch (NoResultException e) {
            result = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        /**                 <ol>
         *     <li>The entity instance</li>
         *     <li>Revision entity, corresponding to the revision where the entity was modified.  If no custom
         *     revision entity is used, this will be an instance of {@link org.hibernate.envers.DefaultRevisionEntity}.</li>
         *     <li>The revision type, an enum of class {@link org.hibernate.envers.RevisionType}.</li>
         *     <li>The names of the properties changed in this revision</li>
         * </ol>
         */
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
        // before we merge an edited revision, the current revision with the id of t is the active rev.
        // after we have merged, the current must be an active revision, either this newly created or the former.

        assert t != null;
        final I id = t.getId();
        assert id != null;
        // read the current entity as being that one which any jpa query will also fetch
        Optional<History<T>> optionalHistory = loadCurrent(id);
        T current = null;
        if (!t.isActiveRevision()) {
            // is there an active version (that then needs to be put back into first position after merge)?
            current = entityManager.find(clz, id);
            entityManager.detach(current);
        }

        T merge = createBean().commitMerge(t);

        if (current != null) {
            pushActiveToTop(id, current);
        }
        return merge;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    private void pushActiveToTop(I id, T current) {
        Optional<History<T>> newCurrent = loadCurrent(id);

        current.setEditedRevision((Integer) newCurrent.get().getRevision());
        entityManager.merge(current);
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
