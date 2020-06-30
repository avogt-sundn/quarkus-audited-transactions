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

    public HistoryList<T, I> getList(@PathParam("id") I id) {
        List<Number> revisions = this.reader.getRevisions(this.clz, id);
        List<History<T, I>> collect = revisions.stream().map(
                rev -> new History<T, I>(
                        this.reader.find((Class<T>) this.clz, (Object) id, rev),
                        rev,
                        this.reader.findRevision(CustomRevisionEntity.class, rev)
                )
        ).collect(Collectors.toList());
        return new HistoryList<T, I>(collect);
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
    public Optional<Historized<T, I>> getSingle(I id) {

        // CustomRevisionEntity of the current version always know if there is an edited version
        Optional<History<T, I>> optionalHistory = loadLatestRevision(id);

        log.info("loaded latest revision: {}", optionalHistory);
        return optionalHistory.map(hy ->
                // we have a revision available, lets produce a Historized summary
                new Historized<T, I>(
                        getActive(hy),
                        getEdited(hy), null
                ));
    }

    private History<T, I> getActive(History<T, I> hy) {
        // only if there has been no active revision yet assigned will we not produce any active history
        if (hy.ref.isActiveRevision()) {
            return hy;
        } else {
            return null;
        }
    }

    private History<T, I> getEdited(History<T, I> hy) {
        if (null != hy && null != hy.ref) {
            if (null != hy.ref.getEditedRevision()) {
                return loadRevision(hy.ref.getId(), hy.ref.getEditedRevision()).orElse(null);
            } else if (!hy.ref.isActiveRevision()) {
                return hy;
            }
        }
        return null;
    }

    /**
     * fetch newest/latest revision to the id.
     * entity is detached since you will never want to change on the revisioned entity.
     * <p>
     * It is possible that there is no revision but a valid entity to the given id:
     * the entity might have been created without hibernate
     *
     * @param id
     * @return the History containing the entity T in a detached state
     */
    private Optional<History<T, I>> loadLatestRevision(I id) {
        // as soon as you use a projection the result will be the revision number
        // we wont do a projection in order to get also the deleted entries
        AuditQuery auditQuery = this.reader.createQuery()
                .forRevisionsOfEntity(this.clz, false, true)
                .add(AuditEntity.id().eq(id))
                // get the highest revision number available by sorting descending plus limiting to 1 entity result
                .addOrder(AuditEntity.revisionNumber().desc())
                .setMaxResults(1);

        Optional<History<T, I>> history = readQuery(id, auditQuery);
        if (history.isEmpty()) {
            T find = findAndDetach(id);
            if (null != find) {
                history = Optional.of(new History<>(find, 0));
            }
        }
        history.ifPresent(hy -> this.entityManager.detach(hy.ref));
        return history;

    }

    /**
     * fetch newest/latest revision to the id
     *
     * @param id
     * @return
     */
    private Optional<History<T, I>> loadRevision(I id, Number revision) {
        AuditQuery auditQuery = this.reader.createQuery()
                .forRevisionsOfEntity(this.clz, false, true)
                .add(AuditEntity.revisionNumber().eq(revision))
                .add(AuditEntity.id().eq(id))
                .setMaxResults(1);

        return readQuery(id, auditQuery);

    }

    private Optional<History<T, I>> readQuery(I id, AuditQuery auditQuery) {

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
                    return new History<T, I>(t, c.getId(), c);
                });
    }

    public void persist(T t) {
        this.entityManager.persist(t);
    }

    /**
     * Before we merge an edited revision, the current revision with the id of t is the active rev.
     * After we have merged, the current must be an active revision, either this newly created or the former.
     *
     * @param id
     * @param t
     * @return
     */
    public T merge(final I id, final T t) {

        assert t != null;
        assert id != null;
        // the latest revision envers has created (there may be none!)
        Optional<History<T, I>> latest = loadLatestRevision(id);

        // merge against latest edited revision.
        // if there is no such, merge against the latest active revision.
        // if there is no such either, merge against the entity found with a fetch.
        // if there is none of any, just store it man!
        Optional<T> mergeAgainst = latest.map(hy -> {
            // read the latest editedRevision from the latest
            Integer editedRevision = latest.get().ref.getEditedRevision();
            if (null != editedRevision) {
                Optional<History<T, I>> latestEdited = loadRevision(id, editedRevision);
                return Optional.of(latestEdited.get().ref);
            } else {
                return Optional.of(latest.get().ref);
            }
        }).orElseGet(() -> {
                    return Optional.<T>ofNullable(findAndDetach(id));
                }
        );
        T merge = mergeAgainst.map(ma -> mergeBeans(t, ma)).orElse(t);
        log.info("em.merge with: {}", merge);
        // commit will save the t and also create a new revision when the tx closes
        merge = createBean().commitMerge(merge);

        if (!t.isActiveRevision())
        // make former latest revision again the latest, pushing the edited revision one behind
        // this way edited revision will never be fetched by any queries.
        {
            latest.ifPresent(
                    hy -> {
                        // get the new edited revision and store it in the new active revision:
                        Optional<History<T, I>> newCurrent = loadLatestRevision(id);
                        hy.ref.setEditedRevision((Integer) newCurrent.get().getRevision());
                        this.entityManager.merge(hy.ref);
                    });
        }
        return merge;
    }

    protected T mergeBeans(T t, T ma) {
        return BeanMerge.merge2On1(ma, t);
    }

    private T findAndDetach(I id) {
        T t1 = this.entityManager.find(this.clz, id);
        if (null != t1) {
            this.entityManager.detach(t1);
        }
        return t1;
    }


    public <I> void delete(I id) {
        this.entityManager.remove(this.entityManager.find(this.clz, id));
    }
}
