package ru.craftysoft.orderingsystem.executor.controller;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.StreamObserver;
import ru.craftysoft.orderingsystem.executor.logic.GetExecutorOperation;
import ru.craftysoft.orderingsystem.executor.logic.UpdateExecutorBalanceOperation;
import ru.craftysoft.orderingsystem.executor.proto.*;
import ru.craftysoft.orderingsystem.executor.proto.Error;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ExecutorController extends ExecutorServiceGrpc.ExecutorServiceImplBase {

    private final UpdateExecutorBalanceOperation updateExecutorBalanceOperation;
    private final GetExecutorOperation getExecutorOperation;

    @Inject
    public ExecutorController(UpdateExecutorBalanceOperation updateExecutorBalanceOperation, GetExecutorOperation getExecutorOperation) {
        this.updateExecutorBalanceOperation = updateExecutorBalanceOperation;
        this.getExecutorOperation = getExecutorOperation;
    }

    @Override
    public void updateExecutorBalance(UpdateExecutorBalanceRequest request, StreamObserver<UpdateExecutorBalanceResponse> responseObserver) {
        updateExecutorBalanceOperation.process(request).whenComplete((response, throwable) -> {
            if (throwable == null) {
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                var errorResponse = UpdateExecutorBalanceResponse.newBuilder()
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
    public void getExecutor(GetExecutorRequest request, StreamObserver<GetExecutorResponse> responseObserver) {
        getExecutorOperation.process(request).whenComplete((response, throwable) -> {
            if (throwable == null) {
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                var errorResponse = UpdateExecutorBalanceResponse.newBuilder()
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
