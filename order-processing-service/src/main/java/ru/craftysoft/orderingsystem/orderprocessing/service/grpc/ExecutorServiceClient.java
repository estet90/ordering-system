package ru.craftysoft.orderingsystem.orderprocessing.service.grpc;

import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import ru.craftysoft.orderingsystem.executor.proto.ExecutorServiceGrpc;
import ru.craftysoft.orderingsystem.executor.proto.UpdateExecutorBalanceRequest;
import ru.craftysoft.orderingsystem.executor.proto.UpdateExecutorBalanceResponse;
import ru.craftysoft.orderingsystem.util.error.exception.BaseException;
import ru.craftysoft.orderingsystem.util.error.exception.InvocationException;
import ru.craftysoft.orderingsystem.util.grpc.LoggingClientInterceptor;
import ru.craftysoft.orderingsystem.util.grpc.MetadataBuilder;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static ru.craftysoft.orderingsystem.orderprocessing.error.exception.InvocationExceptionCode.EXECUTOR_SERVICE;
import static ru.craftysoft.orderingsystem.orderprocessing.error.operation.ModuleOperationCode.resolve;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.newInvocationException;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.newRetryableException;
import static ru.craftysoft.orderingsystem.util.grpc.ExceptionHelper.*;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

@Singleton
@Slf4j
public class ExecutorServiceClient {

    private final ExecutorServiceGrpc.ExecutorServiceStub executorServiceStub;
    private final Metadata.Key<byte[]> updateExecutorBalanceResponseErrorKey = buildKey(UpdateExecutorBalanceResponse.class);
    private final Function<StatusRuntimeException, UpdateExecutorBalanceResponse> errorResponseBuilder = statusRuntimeException -> extractErrorResponse(statusRuntimeException, updateExecutorBalanceResponseErrorKey, UpdateExecutorBalanceResponse::parseFrom);
    private final Function<Throwable, InvocationException> invocationExceptionFactory = throwable -> newInvocationException(throwable, resolve(), EXECUTOR_SERVICE);
    private final Function<Throwable, InvocationException> retryableExceptionFactory = throwable -> newRetryableException(throwable, resolve(), EXECUTOR_SERVICE);
    private final BiConsumer<UpdateExecutorBalanceResponse, BaseException> baseExceptionFiller = (errorResponse, baseException) -> ofNullable(errorResponse)
            .filter(UpdateExecutorBalanceResponse::hasError)
            .map(UpdateExecutorBalanceResponse::getError)
            .ifPresent(error -> baseException
                    .setOriginalCode(error.getCode())
                    .setOriginalMessage(error.getMessage()));

    @Inject
    public ExecutorServiceClient(ExecutorServiceGrpc.ExecutorServiceStub executorServiceStub) {
        this.executorServiceStub = executorServiceStub;
    }

    public CompletableFuture<UpdateExecutorBalanceResponse> updateExecutorBalance(UpdateExecutorBalanceRequest request) {
        var mdc = MDC.getCopyOfContextMap();
        var metadata = MetadataBuilder.build(mdc);
        var result = new CompletableFuture<UpdateExecutorBalanceResponse>();
        executorServiceStub
                .withInterceptors(new LoggingClientInterceptor(metadata))
                .updateExecutorBalance(request, new StreamObserver<>() {
                    private UpdateExecutorBalanceResponse response;
                    private final String point = "ExecutorServiceClient.updateExecutorBalance";

                    @Override
                    public void onNext(UpdateExecutorBalanceResponse response) {
                        this.response = response;
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        var baseException = withMdc(mdc, () -> {
                            log.error("{}.onError.thrown {}", point, throwable.getMessage());
                            return mapException(
                                    log, point, mdc, throwable,
                                    errorResponseBuilder, invocationExceptionFactory, retryableExceptionFactory, baseExceptionFiller
                            );
                        }).get();
                        result.completeExceptionally(baseException);
                    }

                    @Override
                    public void onCompleted() {
                        result.complete(this.response);
                    }
                });
        return result;
    }
}
