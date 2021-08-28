package ru.craftysoft.orderingsystem.gateway.controller;

import ru.craftysoft.orderingsystem.gateway.logic.AddOrderOperation;
import ru.craftysoft.orderingsystem.gateway.logic.GetOrdersOperation;
import ru.craftysoft.orderingsystem.gateway.logic.ProcessOrderOperation;
import ru.craftysoft.orderingsystem.gateway.order.controller.OrdersApi;
import ru.craftysoft.orderingsystem.gateway.order.rest.model.AddOrderRequestData;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Singleton
@Consumes({"application/json;charset=UTF-8"})
@Produces({"application/json;charset=UTF-8"})
@Path("/orders")
public class OrderController implements OrdersApi {

    private final GetOrdersOperation getOrdersOperation;
    private final AddOrderOperation addOrderOperation;
    private final ProcessOrderOperation processOrderOperation;

    @Inject
    public OrderController(GetOrdersOperation getOrdersOperation,
                           AddOrderOperation addOrderOperation,
                           ProcessOrderOperation processOrderOperation) {
        this.getOrdersOperation = getOrdersOperation;
        this.addOrderOperation = addOrderOperation;
        this.processOrderOperation = processOrderOperation;
    }

    @Override
    @RolesAllowed({"customer"})
    public java.util.concurrent.CompletionStage<javax.ws.rs.core.Response> addOrder(String authorization, AddOrderRequestData addOrderRequestData, SecurityContext securityContext) {
        return addOrderOperation.process(authorization, addOrderRequestData)
                .handleAsync((addOrderResponseData, throwable) -> Response.status(Response.Status.CREATED)
                        .entity(addOrderResponseData)
                        .build());
    }

    @Override
    @RolesAllowed({"customer", "executor"})
    public java.util.concurrent.CompletionStage<javax.ws.rs.core.Response> getOrders(SecurityContext securityContext) {
        return getOrdersOperation.process()
                .handleAsync((orders, throwable) -> Response.ok(orders).build());
    }

    @Override
    @RolesAllowed({"executor"})
    public java.util.concurrent.CompletionStage<javax.ws.rs.core.Response> processOrder(Long id, String authorization, SecurityContext securityContext) {
        return processOrderOperation.process(id, authorization)
                .handleAsync((processOrderResponseData, throwable) -> Response.accepted(processOrderResponseData).build());
    }
}
