package org.acme.hibernate.envers.historized.impl;

import io.quarkus.security.identity.SecurityIdentity;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.RevisionListener;

import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class CustomRevisionEntityListener implements RevisionListener {

    public void newRevision(Object revisionEntity) {
        log.info("new revision: {}", revisionEntity);
        CustomRevisionEntity customRevisionEntity =
                (CustomRevisionEntity) revisionEntity;

        SecurityIdentity securityIdentity = IdentityService.get();
        customRevisionEntity.username = securityIdentity.getPrincipal().getName();
        customRevisionEntity.roles = Optional.ofNullable(securityIdentity.getRoles()).get().stream().collect(Collectors.joining(","));
    }
}
