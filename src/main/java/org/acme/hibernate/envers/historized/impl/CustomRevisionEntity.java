package org.acme.hibernate.envers.historized.impl;

import lombok.EqualsAndHashCode;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

import javax.persistence.Entity;

@EqualsAndHashCode(callSuper = true)
@Entity
@RevisionEntity(value = CustomRevisionEntityListener.class)
public class CustomRevisionEntity extends DefaultRevisionEntity {
    public String username;
    /**
     * will be all roles joined into a comma-delimited string
     */
    public String roles;
}
