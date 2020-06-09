package org.acme.hibernate.envers.panache;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.acme.hibernate.envers.historized.api.HistorizedResource;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

;

@Path("fruits")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Transactional
@RequestScoped
@NoArgsConstructor
@Slf4j
public class FruitResource extends HistorizedResource<Fruit, UUID> {

    @Inject
    public FruitResource(EntityManager entityManager) {
        super(Fruit.class, entityManager);
    }

    @GET
    public List<Fruit> list() {
        return Fruit.findAll().list();
    }

    @Override
    public Response add(@Valid final Fruit fruit) {
        Optional.ofNullable(fruit.values).stream().forEach(set -> set.stream().forEach(nu -> {
            nu.fruit = fruit;
            nu.persist();
        }));
        return super.add(fruit);
    }
}
