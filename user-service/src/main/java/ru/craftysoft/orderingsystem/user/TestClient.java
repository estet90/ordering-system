package ru.craftysoft.orderingsystem.user;

import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import ru.craftysoft.orderingsystem.user.proto.GetRolesRequest;
import ru.craftysoft.orderingsystem.user.proto.GetRolesResponse;
import ru.craftysoft.orderingsystem.user.proto.UserServiceGrpc;

public class TestClient {

    public static void main(String[] args) {
        var channel = ManagedChannelBuilder
                .forAddress("localhost", 8091)
                .usePlaintext()
                .build();
        var request = GetRolesRequest.newBuilder()
                .setUserLogin("customer1")
                .setUserPassword("QpFyH6XVg7TRJvD36fSpvQ==")
                .build();
        var stub = UserServiceGrpc.newStub(channel);
        stub.getRoles(request, new StreamObserver<>() {
            @Override
            public void onNext(GetRolesResponse value) {
                System.out.println(value.getGetRolesResponseData());
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("zaebis");
            }
        });
        var blockingStub = UserServiceGrpc.newBlockingStub(channel);
        var response = blockingStub.getRoles(request);
        System.out.println(response);
    }

}
