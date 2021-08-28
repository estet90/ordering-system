package ru.craftysoft.orderingsystem.user.controller;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.StreamObserver;
import ru.craftysoft.orderingsystem.user.logic.GetRolesOperation;
import ru.craftysoft.orderingsystem.user.logic.GetUserIdOperation;
import ru.craftysoft.orderingsystem.user.proto.*;
import ru.craftysoft.orderingsystem.user.proto.Error;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UserController extends UserServiceGrpc.UserServiceImplBase {

    private final GetRolesOperation getRolesOperation;
    private final GetUserIdOperation getUserIdOperation;

    @Inject
    public UserController(GetRolesOperation getRolesOperation, GetUserIdOperation getUserIdOperation) {
        this.getRolesOperation = getRolesOperation;
        this.getUserIdOperation = getUserIdOperation;
    }

    @Override
    public void getRoles(GetRolesRequest request, StreamObserver<GetRolesResponse> responseObserver) {
        getRolesOperation.process(request).whenComplete((getRolesResponse, throwable) -> {
            if (throwable == null) {
                responseObserver.onNext(getRolesResponse);
                responseObserver.onCompleted();
            } else {
                var errorResponse = GetRolesResponse.newBuilder()
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
    public void getUserId(GetUserIdRequest request, StreamObserver<GetUserIdResponse> responseObserver) {
        getUserIdOperation.process(request).whenComplete((getRolesResponse, throwable) -> {
            if (throwable == null) {
                responseObserver.onNext(getRolesResponse);
                responseObserver.onCompleted();
            } else {
                var errorResponse = GetRolesResponse.newBuilder()
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