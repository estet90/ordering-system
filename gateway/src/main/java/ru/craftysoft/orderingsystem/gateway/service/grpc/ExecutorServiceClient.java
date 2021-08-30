package ru.craftysoft.orderingsystem.gateway.service.grpc;

import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import ru.craftysoft.orderingsystem.executor.proto.ExecutorServiceGrpc;
import ru.craftysoft.orderingsystem.executor.proto.GetExecutorRequest;
import ru.craftysoft.orderingsystem.executor.proto.GetExecutorResponse;
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
import static ru.craftysoft.orderingsystem.gateway.error.exception.InvocationExceptionCode.EXECUTOR_SERVICE;
import static ru.craftysoft.orderingsystem.gateway.error.operation.ModuleOperationCode.resolve;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.newInvocationException;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.newRetryableException;
import static ru.craftysoft.orderingsystem.util.grpc.ExceptionHelper.*;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

@Singleton
@Slf4j
public class ExecutorServiceClient {

    private final ExecutorServiceGrpc.ExecutorServiceStub executorServiceStub;
    private final Metadata.Key<byte[]> getExecutorResponseErrorKey = buildKey(GetExecutorResponse.class);
    private final Function<StatusRuntimeException, GetExecutorResponse> errorResponseBuilder = statusRuntimeException -> extractErrorResponse(statusRuntimeException, getExecutorResponseErrorKey, GetExecutorResponse::parseFrom);
    private final Function<Throwable, InvocationException> invocationExceptionFactory = throwable -> newInvocationException(throwable, resolve(), EXECUTOR_SERVICE);
    private final Function<Throwable, InvocationException> retryableExceptionFactory = throwable -> newRetryableException(throwable, resolve(), EXECUTOR_SERVICE);
    private final BiConsumer<GetExecutorResponse, BaseException> baseExceptionFiller = (errorResponse, baseException) -> ofNullable(errorResponse)
            .filter(GetExecutorResponse::hasError)
            .map(GetExecutorResponse::getError)
            .ifPresent(error -> baseException
                    .setOriginalCode(error.getCode())
                    .setOriginalMessage(error.getMessage()));

    @Inject
    public ExecutorServiceClient(ExecutorServiceGrpc.ExecutorServiceStub executorServiceStub) {
        this.executorServiceStub = executorServiceStub;
    }

    public CompletableFuture<GetExecutorResponse> getExecutor(GetExecutorRequest request) {
        var mdc = MDC.getCopyOfContextMap();
        var metadata = MetadataBuilder.build(mdc);
        var result = new CompletableFuture<GetExecutorResponse>();
        executorServiceStub
                .withInterceptors(new LoggingClientInterceptor(metadata))
                .getExecutor(request, new StreamObserver<>() {
                    private GetExecutorResponse response;
                    private final String point = "ExecutorServiceClient.getExecutor";

                    @Override
                    public void onNext(GetExecutorResponse response) {
                        this.response = response;
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        withMdc(mdc, () -> log.error("{}.onError.thrown {}", point, throwable.getMessage()));
                        var baseException = mapException(
                                log, point, mdc, throwable,
                                errorResponseBuilder, invocationExceptionFactory, retryableExceptionFactory, baseExceptionFiller
                        );
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
