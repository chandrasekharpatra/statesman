package io.appform.statesman.server.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import io.appform.statesman.server.ingress.IngressHandler;
import io.appform.statesman.server.ingress.ServiceProviderCallbackHandler;
import io.appform.statesman.server.requests.IngressCallback;
import io.swagger.annotations.Api;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 */
@Path("/callbacks")
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
@Api("Callbacks")
public class Callbacks {

    private final Provider<IngressHandler> ingressHandler;
    private final Provider<ServiceProviderCallbackHandler> providerCallbackHandler;

    @Inject
    public Callbacks(
            Provider<IngressHandler> ingressHandler,
            Provider<ServiceProviderCallbackHandler> providerCallbackHandler) {
        this.ingressHandler = ingressHandler;
        this.providerCallbackHandler = providerCallbackHandler;
    }


    @POST
    @Path("/ingress/final/{ingressProvider}")
    @Consumes(MediaType.APPLICATION_JSON)
    @SneakyThrows
    public Response finalIngressCallback(
            @PathParam("ingressProvider") final String ingressProvider, final IngressCallback ingressCallback) {
        final boolean status = ingressHandler.get()
                .invokeEngineForOneShot(ingressProvider, ingressCallback, true);
        if(!status) {
            log.warn("Ignored ingress provider {} callback: {}", ingressProvider, ingressCallback);
        }
        return Response.ok()
                .entity(ImmutableMap.of("success", status))
                .build();
    }

    @POST
    @Path("/ingress/final/raw/{ingressProvider}")
    @Consumes(MediaType.APPLICATION_JSON)
    @SneakyThrows
    public Response finalIngressCallbackPost(
            @PathParam("ingressProvider") final String ingressProvider, final IngressCallback ingressCallback) {
        final boolean status = ingressHandler.get()
                .invokeEngineForOneShot(ingressProvider, ingressCallback, false);
        if(!status) {
            log.warn("Ignored ingress provider {} callback: {}", ingressProvider, ingressCallback);
        }
        return Response.ok()
                .entity(ImmutableMap.of("success", status))
                .build();
    }

    @POST
    @Path("/ingress/message/{messageProvider}")
    @Consumes(MediaType.APPLICATION_JSON)
    @SneakyThrows
    public Response providerMessageHandlerIngressCallback(
        @PathParam("messageProvider") final String messageProvider, final IngressCallback ingressCallback) {
        final boolean status = ingressHandler.get()
            .invokeEngineForMessageProvider(messageProvider, ingressCallback);
        if(!status) {
            log.warn("Ignored ingress provider {} callback: {}", messageProvider, ingressCallback);
        }
        return Response.ok()
            .entity(ImmutableMap.of("success", status))
            .build();
    }

    @POST
    @Path("/ingress/step/{ingressProvider}")
    @Consumes(MediaType.APPLICATION_JSON)
    @SneakyThrows
    public Response stepIngressCallback(
            @PathParam("ingressProvider") final String ingressProvider, final IngressCallback ingressCallback) {
        final boolean status = ingressHandler.get()
                .invokeEngineForMultiStep(ingressProvider, ingressCallback);
        if(!status) {
            log.warn("Ignored ingress provider {} callback: {}", ingressProvider, ingressCallback);
        }
        return Response.ok()
                .entity(ImmutableMap.of("success", status))
                .build();
    }

    @POST
    @Path("/ingress/obd/{ingressProvider}")
    @Consumes(MediaType.APPLICATION_JSON)
    @SneakyThrows
    public Response stepIngressObdCallback(
            @PathParam("ingressProvider") final String ingressProvider,
            @QueryParam("state") final String state,
            final IngressCallback ingressCallback) {
        final boolean status = ingressHandler.get()
                .invokeEngineForOBDCalls(ingressProvider, state, ingressCallback);
        if(!status) {
            log.warn("Ignored ingress OBD callback from provider {} callback: {}", ingressProvider, ingressCallback);
        }
        return Response.ok()
                .entity(ImmutableMap.of("success", status))
                .build();
    }

    @POST
    @Path("/ingress/form/{provider}")
    @Consumes(MediaType.APPLICATION_JSON)
    @SneakyThrows
    public Response stepIngressForm(
            @PathParam("provider") final String provider, final IngressCallback ingressCallback) {
        final boolean status = ingressHandler.get()
                .invokeEngineForFormPost(provider, ingressCallback);
        if(!status) {
            log.warn("Ignored ingress form post callback from provider {} callback: {}", provider, ingressCallback);
        }
        return Response.ok()
                .entity(ImmutableMap.of("success", status))
                .build();
    }


    @POST
    @Path("/ingress/raw/{provider}")
    @Consumes(MediaType.APPLICATION_JSON)
    @SneakyThrows
    public Response ingressRaw(
            @PathParam("provider") final String provider, final IngressCallback ingressCallback) {
        final boolean status = ingressHandler.get()
                .invokeEngineForRaw(provider, ingressCallback);
        if(!status) {
            log.warn("Ignored ingress form post callback from provider {} callback: {}", provider, ingressCallback);
        }
        return Response.ok()
                .entity(ImmutableMap.of("success", status))
                .build();
    }

    @POST
    @Path("/provider/{serviceProvider}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response providerCallback(
            @PathParam("serviceProvider") final String serviceProvider, JsonNode incomingData) throws Exception {
        final boolean status = providerCallbackHandler.get()
                .handleServiceProviderCallback(serviceProvider, incomingData);
        if(!status) {
            log.warn("Ignored service provider {} callback: {}", serviceProvider, incomingData);
        }
        return Response.ok()
                .entity(ImmutableMap.of("success", status))
                .build();
    }
}
