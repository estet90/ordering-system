package ru.craftysoft.orderingsystem.user.controller;

import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.user.builder.operation.GetRolesResponseBuilder;
import ru.craftysoft.orderingsystem.user.builder.operation.GetUserIdResponseBuilder;
import ru.craftysoft.orderingsystem.user.error.operation.ModuleOperationCode;
import ru.craftysoft.orderingsystem.user.logic.GetRolesOperation;
import ru.craftysoft.orderingsystem.user.logic.GetUserIdOperation;
import ru.craftysoft.orderingsystem.user.proto.*;
import ru.craftysoft.orderingsystem.util.grpc.ExceptionHelper;

import javax.inject.Inject;
import javax.inject.Singleton;

import static ru.craftysoft.orderingsystem.user.error.operation.ModuleOperationCode.GET_ROLES;
import static ru.craftysoft.orderingsystem.user.error.operation.ModuleOperationCode.GET_USER_ID;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.mapException;
import static ru.craftysoft.orderingsystem.util.error.logging.ExceptionLoggerHelper.logError;
import static ru.craftysoft.orderingsystem.util.mdc.MdcKey.OPERATION_NAME_KEY;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withContext;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

@Singleton
@Slf4j
public class UserController extends UserServiceGrpc.UserServiceImplBase {

    private final GetRolesOperation getRolesOperation;
    private final GetUserIdOperation getUserIdOperation;
    private final GetRolesResponseBuilder getRolesResponseBuilder;
    private final GetUserIdResponseBuilder getUserIdResponseBuilder;

    @Inject
    public UserController(GetRolesOperation getRolesOperation,
                          GetUserIdOperation getUserIdOperation,
                          GetRolesResponseBuilder getRolesResponseBuilder,
                          GetUserIdResponseBuilder getUserIdResponseBuilder) {
        this.getRolesOperation = getRolesOperation;
        this.getUserIdOperation = getUserIdOperation;
        this.getRolesResponseBuilder = getRolesResponseBuilder;
        this.getUserIdResponseBuilder = getUserIdResponseBuilder;
    }

    @Override
    public void getRoles(GetRolesRequest request, StreamObserver<GetRolesResponse> responseObserver) {
        var context = Context.current().withValue(OPERATION_NAME_KEY, GET_ROLES.name());
        withContext(context, () -> getRolesOperation.process(request)).whenComplete(withMdc((getRolesResponse, throwable) -> {
            if (throwable == null) {
                responseObserver.onNext(getRolesResponse);
                responseObserver.onCompleted();
            } else {
                var baseException = mapException(throwable, ModuleOperationCode::resolve);
                logError(log, "UserController.getRoles", baseException);
                var errorResponse = getRolesResponseBuilder.build(baseException);
                responseObserver.onError(ExceptionHelper.messageToException(errorResponse));
            }
        }));
    }

    @Override
    public void getUserId(GetUserIdRequest request, StreamObserver<GetUserIdResponse> responseObserver) {
        var context = Context.current().withValue(OPERATION_NAME_KEY, GET_USER_ID.name());
        withContext(context, () -> getUserIdOperation.process(request)).whenComplete(withMdc((getRolesResponse, throwable) -> {
            if (throwable == null) {
                responseObserver.onNext(getRolesResponse);
                responseObserver.onCompleted();
            } else {
                var baseException = mapException(throwable, ModuleOperationCode::resolve);
                logError(log, "UserController.getRoles", baseException);
                var errorResponse = getUserIdResponseBuilder.build(baseException);
                responseObserver.onError(ExceptionHelper.messageToException(errorResponse));
            }
        }));
    }
}
