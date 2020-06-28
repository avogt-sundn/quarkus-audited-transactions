package org.acme.hibernate.envers.historized.api;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.acme.hibernate.envers.historized.impl.HistorizedRepository;
import org.acme.hibernate.envers.historized.impl.HistoryList;

import javax.json.Json;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Optional;

/**
 * @param <T> Entity type
 * @param <I> primary key / id type
 */
@Slf4j
@NoArgsConstructor
public class HistorizedResource<T extends Historizable<I>, I> {

    HistorizedRepository<T, I> repository;
    Class<T> clazz;

    /**
     * We need especially the clazz type at runtime being handed over here.
     * The entityManager methods used depend on this type as one parameter.
     *
     * @param clazz         the java class representing the entity bean type T
     * @param entityManager the entityManager injected in the using class.
     */
    public HistorizedResource(Class<T> clazz, EntityManager entityManager) {
        this.clazz = clazz;
        this.repository = new HistorizedRepository<>(clazz, entityManager);
    }

    @GET
    @Path("{id}/revisions")
    public HistoryList<T, I> getRevisions(@PathParam("id") I id) {
        return this.repository.getList(id);
    }

    @GET
    @Path("{id}")
    public Historized<T, I> getSingle(@PathParam("id") I id) {

        Optional<Historized<T, I>> optional = this.repository.getSingle(id);
        return optional.orElseThrow(()
                -> new WebApplicationException("could not find object to given id: " + id, Response.Status.NOT_FOUND));
    }

    /**
     * POST /Ts - receives a T to store as new T
     * - I of the new object can be set by the client
     * - if not present, I will be generated
     * - body is expected to be valid T json
     *
     * @param t - the T data to be stored
     * @return - the new T
     * - http-201 CREATED
     */
    @POST
    @Transactional
    public Response add(@Valid T t) {
        if (t.getId() == null) {
            t.generateId();
        }
        this.repository.persist(t);
        return Response.ok(t).status(Response.Status.CREATED).build();
    }

    /**
     * PUT /T/{id} - receives a T as an optionally partial and always overwriting update to an already stored T.
     * null values in the received T will be ignored.
     * <p>
     * If there exists no T to the id presented in the @PathParam, a new will be created just as with POST.
     * <p>
     * If you want to merge only selected values, use PATCH instead.
     * - I is expected on the url path
     * - body is expected to be valid T json
     *
     * @param id
     * @param t
     * @return
     */
    @PUT
    @Path("{id}")
    @Transactional
    public Response addOrOverwrite(@PathParam("id") I id, T t) {
        return partialUpdateOrCreate(id, t);
    }

    protected Response partialUpdateOrCreate(I id, T t) {
        if (id == null) {
            throw new WebApplicationException("id was missing on request.", Response.Status.BAD_REQUEST);
        }
        // the id in the path param is authoritative, ignore any id in the body
        t.setId(id);
        // merge will replace all stored values with the ones received - null will overwrite!
        T merged = this.repository.merge(id, t);
        return Response.ok(merged).status(Response.Status.OK).build();
    }

    /**
     * PATCH is like PUT, the idea is for partial updating prefer PATCH.
     * Nevertheless PUT can do the same partial merge.
     * <p>
     * The filled values are then merged into the already existent T entity.
     *
     * @param id
     * @param t
     * @return
     */
    @PATCH
    @Path("{id}")
    @Transactional
    public Response patch(@PathParam("id") I id, T t) {
        return partialUpdateOrCreate(id, t);
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public Response deleteSingle(@PathParam("id") I I) {
        if (I == null) {
            throw new WebApplicationException("id was missing on request.", Response.Status.BAD_REQUEST);
        }
        this.repository.delete(I);
        return Response.ok().status(Response.Status.NO_CONTENT).build();
    }

    @Provider
    public static class ErrorMapper implements ExceptionMapper<Exception> {

        @Override
        public Response toResponse(Exception exception) {
            int code = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
            // WebApplicationException will not produce stacktrace since its expected
            if (exception instanceof WebApplicationException) {
                code = ((WebApplicationException) exception).getResponse().getStatus();
            } else {
                log.error("", exception);
            }
            return Response.status(code)
                    .entity(Json.createObjectBuilder().add("error", "" + exception.getMessage()).add("code", code).build())
                    .build();
        }

    }
}
