package org.acme.hibernate.envers.historized.impl;

import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;


@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@RevisionEntity(value = CustomRevisionEntityListener.class)
public class CustomRevisionEntity extends DefaultRevisionEntity {
    String username;
    /**
     * will be all roles joined into a comma-delimited string
     */
    String roles;
}
