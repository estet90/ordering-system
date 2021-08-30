package ru.craftysoft.orderingsystem.order.controller;

import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.order.builder.operation.AddOrderResponseBuilder;
import ru.craftysoft.orderingsystem.order.builder.operation.GetOrdersResponseBuilder;
import ru.craftysoft.orderingsystem.order.builder.operation.ReserveOrderResponseBuilder;
import ru.craftysoft.orderingsystem.order.error.operation.ModuleOperationCode;
import ru.craftysoft.orderingsystem.order.logic.AddOrderOperation;
import ru.craftysoft.orderingsystem.order.logic.GetOrdersOperation;
import ru.craftysoft.orderingsystem.order.logic.ReserveOrderOperation;
import ru.craftysoft.orderingsystem.order.proto.*;
import ru.craftysoft.orderingsystem.util.grpc.ExceptionHelper;

import javax.inject.Inject;
import javax.inject.Singleton;

import static ru.craftysoft.orderingsystem.order.error.operation.ModuleOperationCode.*;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.mapException;
import static ru.craftysoft.orderingsystem.util.error.logging.ExceptionLoggerHelper.logError;
import static ru.craftysoft.orderingsystem.util.mdc.MdcKey.OPERATION_NAME_KEY;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withContext;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

@Singleton
@Slf4j
public class OrderController extends OrderServiceGrpc.OrderServiceImplBase {

    private final GetOrdersOperation getOrdersOperation;
    private final AddOrderOperation addOrderOperation;
    private final ReserveOrderOperation reserveOrderOperation;
    private final GetOrdersResponseBuilder getOrdersResponseBuilder;
    private final AddOrderResponseBuilder addOrderResponseBuilder;
    private final ReserveOrderResponseBuilder reserveOrderResponseBuilder;

    @Inject
    public OrderController(GetOrdersOperation getOrdersOperation,
                           AddOrderOperation addOrderOperation,
                           ReserveOrderOperation reserveOrderOperation,
                           GetOrdersResponseBuilder getOrdersResponseBuilder,
                           AddOrderResponseBuilder addOrderResponseBuilder,
                           ReserveOrderResponseBuilder reserveOrderResponseBuilder) {
        this.getOrdersOperation = getOrdersOperation;
        this.addOrderOperation = addOrderOperation;
        this.reserveOrderOperation = reserveOrderOperation;
        this.getOrdersResponseBuilder = getOrdersResponseBuilder;
        this.addOrderResponseBuilder = addOrderResponseBuilder;
        this.reserveOrderResponseBuilder = reserveOrderResponseBuilder;
    }

    @Override
    public void getOrders(GetOrdersRequest request, StreamObserver<GetOrdersResponse> responseObserver) {
        var context = Context.current().withValue(OPERATION_NAME_KEY, GET_ORDERS.name());
        withContext(context, getOrdersOperation::process).whenComplete(withMdc((response, throwable) -> {
            if (throwable == null) {
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                var baseException = mapException(throwable, ModuleOperationCode::resolve);
                logError(log, "OrderController.getOrders", baseException);
                var errorResponse = getOrdersResponseBuilder.build(baseException);
                responseObserver.onError(ExceptionHelper.messageToException(errorResponse));
            }
        }));
    }

    @Override
    public void addOrder(AddOrderRequest request, StreamObserver<AddOrderResponse> responseObserver) {
        var context = Context.current().withValue(OPERATION_NAME_KEY, ADD_ORDER.name());
        withContext(context, () -> addOrderOperation.process(request)).whenComplete(withMdc((response, throwable) -> {
            if (throwable == null) {
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                var baseException = mapException(throwable, ModuleOperationCode::resolve);
                logError(log, "OrderController.addOrder", baseException);
                var errorResponse = addOrderResponseBuilder.build(baseException);
                responseObserver.onError(ExceptionHelper.messageToException(errorResponse));
            }
        }));
    }

    @Override
    public void reserveOrder(ReserveOrderRequest request, StreamObserver<ReserveOrderResponse> responseObserver) {
        var context = Context.current().withValue(OPERATION_NAME_KEY, RESERVE_ORDER.name());
        withContext(context, () -> reserveOrderOperation.process(request)).whenComplete(withMdc((response, throwable) -> {
            if (throwable == null) {
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                var baseException = mapException(throwable, ModuleOperationCode::resolve);
                logError(log, "OrderController.reserveOrder", baseException);
                var errorResponse = reserveOrderResponseBuilder.build(baseException);
                responseObserver.onError(ExceptionHelper.messageToException(errorResponse));
            }
        }));
    }
}
