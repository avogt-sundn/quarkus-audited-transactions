package org.acme.hibernate.orm.envers;


import lombok.Data;
import lombok.NonNull;
import org.acme.hibernate.orm.panache.Fruit;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("fruits")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
@Transactional
public class AuditResource {

    @Inject
    EntityManager entityManager;

    @GET
    @Path("{id}/revisions")
    public HistoryList<Fruit> getSingle(@PathParam("id") UUID id) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        List<Number> revisions = reader.getRevisions(Fruit.class, id);
        List<History<Fruit>> collect = revisions.stream().map(
                rev -> new History<Fruit>(rev,
                        reader.findRevision(CustomRevisionEntity.class, rev),
                        reader.find(Fruit.class, id, rev))).collect(Collectors.toList());
        return new HistoryList<Fruit>(collect);
    }

    @Data
    public static class HistoryList<T> {
        @NonNull
        LocalDateTime fetchDate = LocalDateTime.now();
        @NonNull
        List<History<T>> history;
    }

    @Data
    public static class History<T> {

        @NonNull
        Number revision;
        @NonNull
        CustomRevisionEntity info;
        @NonNull
        T ref;
    }
}
