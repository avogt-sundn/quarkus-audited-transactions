package org.acme.hibernate.envers.panache;

import java.util.UUID;

import org.acme.hibernate.envers.historized.api.HistorizedResource;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.NoArgsConstructor;

;

@Path("fruits")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Transactional
@RequestScoped
@NoArgsConstructor
public class FruitResource extends HistorizedResource<Fruit, UUID> {

    @Inject
    public FruitResource(EntityManager entityManager) {
        super(Fruit.class, entityManager);
    }

    @GET
    @Path("{id}/nohist")
    public Fruit getById(@PathParam("id") UUID id) {
        return Fruit.findById(id);
    }

}
