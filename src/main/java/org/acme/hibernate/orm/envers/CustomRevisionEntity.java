package org.acme.hibernate.orm.envers;

import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

import javax.persistence.Entity;

@Entity(name = "CustomRevisionEntity")
@RevisionEntity(CustomRevisionEntityListener.class)
public class CustomRevisionEntity extends DefaultRevisionEntity {

    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
