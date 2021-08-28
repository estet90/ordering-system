package ru.craftysoft.orderingsystem.gateway.service.grpc;

import io.grpc.stub.StreamObserver;
import ru.craftysoft.orderingsystem.executor.proto.ExecutorServiceGrpc;
import ru.craftysoft.orderingsystem.executor.proto.GetExecutorRequest;
import ru.craftysoft.orderingsystem.executor.proto.GetExecutorResponse;

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

    public CompletableFuture<GetExecutorResponse> getExecutor(GetExecutorRequest request) {
        var result = new CompletableFuture<GetExecutorResponse>();
        executorServiceStub.getExecutor(request, new StreamObserver<>() {
            private GetExecutorResponse response;

            @Override
            public void onNext(GetExecutorResponse response) {
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
