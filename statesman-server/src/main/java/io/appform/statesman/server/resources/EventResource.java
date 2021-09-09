package io.appform.statesman.server.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.appform.eventingester.client.EventPublisher;
import io.appform.eventingester.models.Event;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author shashank.g
 */
@Produces(MediaType.APPLICATION_JSON)
@Path("/v1/events")
@Slf4j
@Api("Event APIs")
@Singleton
public class EventResource {

    private EventPublisher publisher;

    @Inject
    public EventResource(@Named("eventPublisher") EventPublisher publisher) {
        this.publisher = publisher;
    }

    @POST
    @Timed
    @Path("/reporting/publish")
    @ApiOperation("publish event")
    public Response publish(@Valid final Event event) throws Exception {
        publisher.publish(event);
        return Response.ok().build();
    }
}
