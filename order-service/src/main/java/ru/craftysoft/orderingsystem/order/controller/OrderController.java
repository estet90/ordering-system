package ru.craftysoft.orderingsystem.order.controller;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.StreamObserver;
import ru.craftysoft.orderingsystem.order.logic.AddOrderOperation;
import ru.craftysoft.orderingsystem.order.logic.GetOrdersOperation;
import ru.craftysoft.orderingsystem.order.logic.ReserveOrderOperation;
import ru.craftysoft.orderingsystem.order.proto.*;
import ru.craftysoft.orderingsystem.order.proto.Error;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class OrderController extends OrderServiceGrpc.OrderServiceImplBase {

    private final GetOrdersOperation getOrdersOperation;
    private final AddOrderOperation addOrderOperation;
    private final ReserveOrderOperation reserveOrderOperation;

    @Inject
    public OrderController(GetOrdersOperation getOrdersOperation,
                           AddOrderOperation addOrderOperation,
                           ReserveOrderOperation reserveOrderOperation) {
        this.getOrdersOperation = getOrdersOperation;
        this.addOrderOperation = addOrderOperation;
        this.reserveOrderOperation = reserveOrderOperation;
    }

    @Override
    public void getOrders(GetOrdersRequest request, StreamObserver<GetOrdersResponse> responseObserver) {
        getOrdersOperation.process(request).whenComplete((response, throwable) -> {
            if (throwable == null) {
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                var errorResponse = GetOrdersResponse.newBuilder()
                        .setError(Error.newBuilder()
                                .setCode("error")
                                .build()
                        ).build();
                var responseKey = ProtoUtils.keyForProto(errorResponse);
                var metadata = new Metadata();
                metadata.put(responseKey, errorResponse);
                responseObserver.onError(Status.INTERNAL.asRuntimeException(metadata));
            }
        });
    }

    @Override
    public void addOrder(AddOrderRequest request, StreamObserver<AddOrderResponse> responseObserver) {
        addOrderOperation.process(request).whenComplete((response, throwable) -> {
            if (throwable == null) {
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                var errorResponse = GetOrdersResponse.newBuilder()
                        .setError(Error.newBuilder()
                                .setCode("error")
                                .build()
                        ).build();
                var responseKey = ProtoUtils.keyForProto(errorResponse);
                var metadata = new Metadata();
                metadata.put(responseKey, errorResponse);
                responseObserver.onError(Status.INTERNAL.asRuntimeException(metadata));
            }
        });
    }

    @Override
    public void reserveOrder(ReserveOrderRequest request, StreamObserver<ReserveOrderResponse> responseObserver) {
        reserveOrderOperation.process(request).whenComplete((response, throwable) -> {
            if (throwable == null) {
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                var errorResponse = GetOrdersResponse.newBuilder()
                        .setError(Error.newBuilder()
                                .setCode("error")
                                .build()
                        ).build();
                var responseKey = ProtoUtils.keyForProto(errorResponse);
                var metadata = new Metadata();
                metadata.put(responseKey, errorResponse);
                responseObserver.onError(Status.INTERNAL.asRuntimeException(metadata));
            }
        });
    }
}
