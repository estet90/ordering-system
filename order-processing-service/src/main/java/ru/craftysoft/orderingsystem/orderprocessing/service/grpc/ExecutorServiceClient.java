package ru.craftysoft.orderingsystem.orderprocessing.service.grpc;

import io.grpc.stub.StreamObserver;
import ru.craftysoft.orderingsystem.executor.proto.ExecutorServiceGrpc;
import ru.craftysoft.orderingsystem.executor.proto.UpdateExecutorBalanceRequest;
import ru.craftysoft.orderingsystem.executor.proto.UpdateExecutorBalanceResponse;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class ExecutorServiceClient {

    private final ExecutorServiceGrpc.ExecutorServiceStub executorServiceStub;

    @Inject
    public ExecutorServiceClient(ExecutorServiceGrpc.ExecutorServiceStub executorServiceStub) {
        this.executorServiceStub = executorServiceStub;
    }

    public CompletableFuture<UpdateExecutorBalanceResponse> updateExecutorBalance(UpdateExecutorBalanceRequest request) {
        var result = new CompletableFuture<UpdateExecutorBalanceResponse>();
        executorServiceStub.updateExecutorBalance(request, new StreamObserver<>() {
            private UpdateExecutorBalanceResponse response;

            @Override
            public void onNext(UpdateExecutorBalanceResponse response) {
                this.response = response;
            }

            @Override
            public void onError(Throwable throwable) {
                result.completeExceptionally(throwable);
            }

            @Override
            public void onCompleted() {
                result.complete(this.response);
            }
        });
        return result;
    }
}
