package org.acme.hibernate.orm.panache;

import io.quarkus.hibernate.orm.panache.Panache;
import lombok.extern.slf4j.Slf4j;
import org.acme.hibernate.orm.envers.HistorizedRepository;
import org.acme.hibernate.orm.envers.HistoryList;
import org.acme.hibernate.orm.historized.Historized;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

;

@Path("fruits")
@Produces("application/json")
@Consumes("application/json")
@Transactional
@RequestScoped
@Slf4j
public class FruitResource {

    HistorizedRepository<Fruit> repository;

    @Inject
    public FruitResource(EntityManager entityManager) {
        this.repository = new HistorizedRepository<>(Fruit.class, entityManager);
    }

    @GET
    public List<Fruit> list() {
        return Fruit.findAll().list();
    }

    @GET
    @Path("{id}/revisions")
    public HistoryList<Fruit> getRevisions(@PathParam("id") UUID id) {
        return repository.getList(id);
    }

    @GET
    @Path("{id}")
    public Historized<Fruit> getSingle(@PathParam("id") UUID id) {
        Optional<Historized<Fruit>> optional = repository.getSingle(id);
        return optional.orElseThrow(()
                -> new WebApplicationException("could not find object to given id: " + id, Response.Status.NOT_FOUND));
    }

    /**
     * POST /fruits - receives a Fruit to store as new Fruit
     * - uuid of the new object can be set by the client
     * - if not present, uuid will be generated
     * - body is expected to be valid Fruit json
     *
     * @param fruit - the fruit data to be stored
     * @return - the new Fruit
     * - http-201 CREATED
     */
    @POST
    @Transactional
    public Response add(@Valid Fruit fruit) {
        if (fruit.id == null) {
            fruit.id = UUID.randomUUID();
        }

        Fruit.persist(fruit);
        return Response.ok(fruit).status(Response.Status.CREATED).build();
    }


    /**
     * PUT /fruit/{id} - receives a Fruit as an update to an already stored Fruit
     * - uuid is expected on the url path
     * - body is expected to be valid Fruit json
     *
     * @param id
     * @param fruit
     * @return
     */
    @PUT
    @Path("{id}")
    @Transactional
    public Response addOrUpdate(@PathParam("id") UUID id, @Valid Fruit fruit) {
        if (id == null) {
            throw new WebApplicationException("uuid was missing on request.", Response.Status.BAD_REQUEST);
        }
        fruit.id = id;
        // merge will replace all stored values with the ones received - null will overwrite!
        Fruit merged = Panache.getEntityManager().merge(fruit);
        return Response.ok(merged).status(Response.Status.CREATED).build();
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public Response deleteSingle(@PathParam("id") UUID uuid) {
        if (uuid == null) {
            throw new WebApplicationException("Id was missing on request.", Response.Status.BAD_REQUEST);
        }
        Fruit.delete("id", uuid);
        return Response.ok().status(Response.Status.NO_CONTENT).build();
    }

    @Provider
    public static class ErrorMapper implements ExceptionMapper<Exception> {

        @Override
        public Response toResponse(Exception exception) {
            log.error("ErrorMapper: {}", exception);
            int code = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
            if (exception instanceof WebApplicationException) {
                code = ((WebApplicationException) exception).getResponse().getStatus();
            }

            return Response.status(code)
                    .entity(Json.createObjectBuilder().add("error", "" + exception.getMessage()).add("code", code).build())
                    .build();
        }

    }
}
