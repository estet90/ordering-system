package ru.craftysoft.orderingsystem.gateway.controller;

import io.vertx.core.Vertx;
import ru.craftysoft.orderingsystem.gateway.logic.GetOrdersOperation;
import ru.craftysoft.orderingsystem.util.jackson.Jackson;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;

@Singleton
@Path("/orders")
public class OrderController implements OrdersApi {

    private final GetOrdersOperation getOrdersOperation;
    private final Jackson jackson;

    @Inject
    public OrderController(GetOrdersOperation getOrdersOperation, Jackson jackson) {
        this.getOrdersOperation = getOrdersOperation;
        this.jackson = jackson;
    }

    @Override
    @RolesAllowed("customer")
    public void getOrders(String authorization, AsyncResponse asyncResponse, Vertx vertx) {
        getOrdersOperation.process().whenComplete((orders, throwable) -> {
            if (throwable != null) {
                asyncResponse.resume(throwable);
            } else {
                asyncResponse.resume(jackson.toString(orders));
            }
        });
    }
}
