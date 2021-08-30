package ru.craftysoft.orderingsystem.gateway.controller;

import org.jboss.resteasy.spi.HttpRequest;
import org.slf4j.MDC;
import ru.craftysoft.orderingsystem.gateway.error.operation.ModuleOperationCode;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import static java.util.Optional.ofNullable;
import static ru.craftysoft.orderingsystem.gateway.error.operation.ModuleOperationCode.*;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.mapException;
import static ru.craftysoft.orderingsystem.util.mdc.MdcKey.OPERATION_NAME;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

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
    public CompletionStage<Response> addOrder(String authorization, AddOrderRequestData addOrderRequestData, org.jboss.resteasy.spi.HttpRequest request) {
        try {
            mdcInit(request, ADD_ORDER);
            return addOrderOperation.process(authorization, addOrderRequestData)
                    .handleAsync(withMdc((addOrderResponseData, throwable) -> {
                        if (throwable != null) {
                            throw mapException(throwable, ModuleOperationCode::resolve);
                        }
                        return Response.status(Response.Status.CREATED)
                                .entity(addOrderResponseData)
                                .build();
                    }));
        } finally {
            MDC.clear();
        }
    }

    @Override
    @RolesAllowed({"customer", "executor"})
    public CompletionStage<Response> getOrders(org.jboss.resteasy.spi.HttpRequest request) {
        try {
            mdcInit(request, GET_ORDERS);
            return getOrdersOperation.process()
                    .handleAsync(withMdc((orders, throwable) -> {
                        if (throwable != null) {
                            throw mapException(throwable, ModuleOperationCode::resolve);
                        }
                        return Response.ok(orders).build();
                    }));
        } finally {
            MDC.clear();
        }
    }

    @Override
    @RolesAllowed({"executor"})
    public CompletionStage<Response> processOrder(Long id, String authorization, org.jboss.resteasy.spi.HttpRequest request) {
        try {
            mdcInit(request, PROCESS_ORDER);
            return processOrderOperation.process(id, authorization)
                    .handleAsync(withMdc((processOrderResponseData, throwable) -> {
                        if (throwable != null) {
                            throw mapException(throwable, ModuleOperationCode::resolve);
                        }
                        return Response.accepted(processOrderResponseData).build();
                    }));
        } finally {
            MDC.clear();
        }
    }

    private void mdcInit(HttpRequest request, ModuleOperationCode operationCode) {
        ofNullable(request.getAttribute("mdc"))
                .map(mdc -> {
                    try {
                        return (Map<String, String>) mdc;
                    } catch (Exception e) {
                        return null;
                    }
                })
                .ifPresentOrElse(mdc -> {
                    var newMdc = new HashMap<>(mdc);
                    newMdc.put(OPERATION_NAME, operationCode.name());
                    MDC.setContextMap(newMdc);
                }, MDC::clear);
    }
}
