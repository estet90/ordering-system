package ru.craftysoft.orderingsystem.order.controller;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.StreamObserver;
import ru.craftysoft.orderingsystem.order.logic.GetOrdersOperation;
import ru.craftysoft.orderingsystem.order.proto.Error;
import ru.craftysoft.orderingsystem.order.proto.GetOrdersRequest;
import ru.craftysoft.orderingsystem.order.proto.GetOrdersResponse;
import ru.craftysoft.orderingsystem.order.proto.OrderServiceGrpc;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class OrderController extends OrderServiceGrpc.OrderServiceImplBase {

    private final GetOrdersOperation getOrdersOperation;

    @Inject
    public OrderController(GetOrdersOperation getOrdersOperation) {
        this.getOrdersOperation = getOrdersOperation;
    }

    @Override
    public void getOrders(GetOrdersRequest request, StreamObserver<GetOrdersResponse> responseObserver) {
        getOrdersOperation.process(request).whenComplete((getRolesResponse, throwable) -> {
            if (throwable == null) {
                responseObserver.onNext(getRolesResponse);
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
