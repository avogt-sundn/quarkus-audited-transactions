package org.acme.hibernate.envers.historized.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

@Named(Transactor.TRANSACTOR)
@ApplicationScoped
@Transactional(Transactional.TxType.REQUIRES_NEW)
public class Transactor<T> {
    public static final String TRANSACTOR = "transactor";
    @Inject
    EntityManager entityManager;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    protected T commitMerge(T t) {
        entityManager.clear();
        T merge = entityManager.merge(t);
        entityManager.flush();
        return merge;
    }

}
