package org.acme.hibernate.envers.panache;

import java.util.List;
import java.util.UUID;

import org.acme.hibernate.envers.historized.api.HistorizedResource;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
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

    /**
     * You will not want to pull this get-all into the HistorizedResource,
     * because what is it really good for fetching the complete data set if only for drowning in the abundance.
     *
     * We have it around for the test suite, and tests only.
     * @return all fruits that are stored
     */
    @GET
    public List<Fruit> list() {
        return Fruit.findAll().list();
    }


}
