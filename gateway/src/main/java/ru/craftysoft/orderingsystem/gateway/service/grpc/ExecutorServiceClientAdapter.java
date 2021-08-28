package ru.craftysoft.orderingsystem.gateway.service.grpc;

import ru.craftysoft.orderingsystem.executor.proto.GetExecutorRequest;
import ru.craftysoft.orderingsystem.executor.proto.GetExecutorResponse;
import ru.craftysoft.orderingsystem.user.proto.GetUserIdResponse;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class ExecutorServiceClientAdapter {

    private final ExecutorServiceClient client;

    @Inject
    public ExecutorServiceClientAdapter(ExecutorServiceClient client) {
        this.client = client;
    }

    public CompletableFuture<GetExecutorResponse> getExecutor(GetUserIdResponse getUserIdResponse) {
        var request = GetExecutorRequest.newBuilder()
                .setUserId(getUserIdResponse.getGetUserResponseData().getId())
                .build();
        return client.getExecutor(request);
    }
}
