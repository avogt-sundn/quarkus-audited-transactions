package org.acme.hibernate.orm.envers;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.RevisionListener;

@Slf4j
public class CustomRevisionEntityListener implements RevisionListener {

    public void newRevision(Object revisionEntity) {
        log.info("new revision: {}", revisionEntity);
        CustomRevisionEntity customRevisionEntity =
                (CustomRevisionEntity) revisionEntity;

        customRevisionEntity.setUsername(
                "your-name-" + ((CustomRevisionEntity) revisionEntity).getRevisionDate()
        );
    }
}