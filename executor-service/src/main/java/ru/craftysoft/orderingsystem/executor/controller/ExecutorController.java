package ru.craftysoft.orderingsystem.executor.controller;

import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.executor.builder.operation.GetExecutorResponseBuilder;
import ru.craftysoft.orderingsystem.executor.builder.operation.UpdateExecutorBalanceResponseBuilder;
import ru.craftysoft.orderingsystem.executor.error.operation.ModuleOperationCode;
import ru.craftysoft.orderingsystem.executor.logic.GetExecutorOperation;
import ru.craftysoft.orderingsystem.executor.logic.UpdateExecutorBalanceOperation;
import ru.craftysoft.orderingsystem.executor.proto.*;
import ru.craftysoft.orderingsystem.util.grpc.ExceptionHelper;

import javax.inject.Inject;
import javax.inject.Singleton;

import static ru.craftysoft.orderingsystem.executor.error.operation.ModuleOperationCode.GET_EXECUTOR;
import static ru.craftysoft.orderingsystem.executor.error.operation.ModuleOperationCode.UPDATE_EXECUTOR_BALANCE;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.mapException;
import static ru.craftysoft.orderingsystem.util.error.logging.ExceptionLoggerHelper.logError;
import static ru.craftysoft.orderingsystem.util.mdc.MdcKey.OPERATION_NAME_KEY;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withContext;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

@Singleton
@Slf4j
public class ExecutorController extends ExecutorServiceGrpc.ExecutorServiceImplBase {

    private final UpdateExecutorBalanceOperation updateExecutorBalanceOperation;
    private final GetExecutorOperation getExecutorOperation;
    private final UpdateExecutorBalanceResponseBuilder updateExecutorBalanceResponseBuilder;
    private final GetExecutorResponseBuilder getExecutorResponseBuilder;

    @Inject
    public ExecutorController(UpdateExecutorBalanceOperation updateExecutorBalanceOperation,
                              GetExecutorOperation getExecutorOperation,
                              UpdateExecutorBalanceResponseBuilder updateExecutorBalanceResponseBuilder,
                              GetExecutorResponseBuilder getExecutorResponseBuilder) {
        this.updateExecutorBalanceOperation = updateExecutorBalanceOperation;
        this.getExecutorOperation = getExecutorOperation;
        this.updateExecutorBalanceResponseBuilder = updateExecutorBalanceResponseBuilder;
        this.getExecutorResponseBuilder = getExecutorResponseBuilder;
    }

    @Override
    public void updateExecutorBalance(UpdateExecutorBalanceRequest request, StreamObserver<UpdateExecutorBalanceResponse> responseObserver) {
        var context = Context.current().withValue(OPERATION_NAME_KEY, UPDATE_EXECUTOR_BALANCE.name());
        withContext(context, () -> updateExecutorBalanceOperation.process(request)).whenComplete(withMdc((response, throwable) -> {
            if (throwable == null) {
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                var baseException = mapException(throwable, ModuleOperationCode::resolve);
                logError(log, "ExecutorController.updateExecutorBalance", baseException);
                var errorResponse = updateExecutorBalanceResponseBuilder.build(baseException);
                responseObserver.onError(ExceptionHelper.messageToException(errorResponse));
            }
        }));
    }

    @Override
    public void getExecutor(GetExecutorRequest request, StreamObserver<GetExecutorResponse> responseObserver) {
        var context = Context.current().withValue(OPERATION_NAME_KEY, GET_EXECUTOR.name());
        withContext(context, () -> getExecutorOperation.process(request)).whenComplete(withMdc((response, throwable) -> {
            if (throwable == null) {
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                var baseException = mapException(throwable, ModuleOperationCode::resolve);
                logError(log, "ExecutorController.getExecutor", baseException);
                var errorResponse = getExecutorResponseBuilder.build(baseException);
                responseObserver.onError(ExceptionHelper.messageToException(errorResponse));
            }
        }));
    }
}
