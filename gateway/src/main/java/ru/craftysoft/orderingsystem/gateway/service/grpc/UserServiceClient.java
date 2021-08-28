package ru.craftysoft.orderingsystem.gateway.service.grpc;

import io.grpc.stub.StreamObserver;
import ru.craftysoft.orderingsystem.user.proto.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class UserServiceClient {

    private final UserServiceGrpc.UserServiceStub userServiceStub;

    @Inject
    public UserServiceClient(UserServiceGrpc.UserServiceStub userServiceStub) {
        this.userServiceStub = userServiceStub;
    }

    public CompletableFuture<GetRolesResponse> getRoles(GetRolesRequest request) {
        var result = new CompletableFuture<GetRolesResponse>();
        userServiceStub.getRoles(request, new StreamObserver<>() {
            private GetRolesResponse response;

            @Override
            public void onNext(GetRolesResponse response) {
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

    public CompletableFuture<GetUserIdResponse> getUserId(GetUserIdRequest request) {
        var result = new CompletableFuture<GetUserIdResponse>();
        userServiceStub.getUserId(request, new StreamObserver<>() {
            private GetUserIdResponse response;

            @Override
            public void onNext(GetUserIdResponse response) {
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
