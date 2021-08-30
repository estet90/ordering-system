package ru.craftysoft.orderingsystem.gateway.controller;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.util.Objects.requireNonNull;

@Singleton
@Path("/swagger")
public class SwaggerController {

    private final String swagger;

    @Inject
    public SwaggerController() {
        try {
            this.swagger = new String(requireNonNull(getClass().getResourceAsStream("/openapi/order.yaml")).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Produces("text/yaml;charset=UTF-8")
    public CompletionStage<Response> swagger(@Context org.jboss.resteasy.spi.HttpRequest request) {
        return CompletableFuture.completedFuture(Response.ok(swagger).build());
    }
}
