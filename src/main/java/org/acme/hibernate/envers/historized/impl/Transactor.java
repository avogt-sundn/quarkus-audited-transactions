package org.acme.hibernate.envers.historized.impl;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@Named(Transactor.TRANSACTOR)
@Dependent
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
