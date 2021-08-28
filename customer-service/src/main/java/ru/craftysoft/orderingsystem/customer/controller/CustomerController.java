package ru.craftysoft.orderingsystem.customer.controller;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.StreamObserver;
import ru.craftysoft.orderingsystem.customer.logic.GetCustomerOperation;
import ru.craftysoft.orderingsystem.customer.logic.UpdateCustomerBalanceOperation;
import ru.craftysoft.orderingsystem.customer.proto.*;
import ru.craftysoft.orderingsystem.customer.proto.Error;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CustomerController extends CustomerServiceGrpc.CustomerServiceImplBase {

    private final GetCustomerOperation getCustomerOperation;
    private final UpdateCustomerBalanceOperation updateCustomerBalanceOperation;

    @Inject
    public CustomerController(GetCustomerOperation getCustomerOperation,
                              UpdateCustomerBalanceOperation updateCustomerBalanceOperation) {
        this.getCustomerOperation = getCustomerOperation;
        this.updateCustomerBalanceOperation = updateCustomerBalanceOperation;
    }

    @Override
    public void getCustomer(GetCustomerRequest request, StreamObserver<GetCustomerResponse> responseObserver) {
        getCustomerOperation.process(request).whenComplete((response, throwable) -> {
            if (throwable == null) {
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                var errorResponse = GetCustomerResponse.newBuilder()
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
    public void updateCustomerBalance(UpdateCustomerBalanceRequest request, StreamObserver<UpdateCustomerBalanceResponse> responseObserver) {
        updateCustomerBalanceOperation.process(request).whenComplete((response, throwable) -> {
            if (throwable == null) {
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                var errorResponse = GetCustomerResponse.newBuilder()
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
