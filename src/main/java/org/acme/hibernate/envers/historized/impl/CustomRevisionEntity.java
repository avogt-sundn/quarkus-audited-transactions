package org.acme.hibernate.envers.historized.impl;

import lombok.Data;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

import javax.persistence.Entity;

@Entity(name = "CustomRevisionEntity")
@RevisionEntity(CustomRevisionEntityListener.class)
@Data
public class CustomRevisionEntity extends DefaultRevisionEntity {
    String username;
    String role;
}
