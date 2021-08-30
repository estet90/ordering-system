package ru.craftysoft.orderingsystem.orderprocessing.service.grpc;

import ru.craftysoft.orderingsystem.orderprocessing.builder.grpc.UpdateExecutorBalanceResponse;
import ru.craftysoft.orderingsystem.orderprocessing.proto.DecreaseExecutorAmountRequest;
import ru.craftysoft.orderingsystem.orderprocessing.proto.IncrementExecutorAmountRequest;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.executor.proto.UpdateExecutorBalanceResponseData.Result.BALANCE_HAS_NOT_BEEN_CHANGED;
import static ru.craftysoft.orderingsystem.orderprocessing.error.exception.BusinessExceptionCode.EXECUTOR_BALANCE_HAS_NOT_BEEN_DECREASED;
import static ru.craftysoft.orderingsystem.orderprocessing.error.exception.BusinessExceptionCode.EXECUTOR_BALANCE_HAS_NOT_BEEN_INCREMENTED;
import static ru.craftysoft.orderingsystem.orderprocessing.error.operation.ModuleOperationCode.resolve;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.newBusinessException;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

@Singleton
public class ExecutorServiceClientAdapter {

    private final ExecutorServiceClient client;
    private final UpdateExecutorBalanceResponse requestBuilder;

    @Inject
    public ExecutorServiceClientAdapter(ExecutorServiceClient client, UpdateExecutorBalanceResponse requestBuilder) {
        this.client = client;
        this.requestBuilder = requestBuilder;
    }

    public CompletableFuture<ru.craftysoft.orderingsystem.executor.proto.UpdateExecutorBalanceResponse> incrementAmount(IncrementExecutorAmountRequest incrementExecutorAmountRequest) {
        var request = requestBuilder.build(incrementExecutorAmountRequest);
        return client.updateExecutorBalance(request)
                .thenApply(withMdc((updateExecutorBalanceResponse) -> {
                    if (BALANCE_HAS_NOT_BEEN_CHANGED.equals(updateExecutorBalanceResponse.getUpdateExecutorBalanceResponseData().getResult())) {
                        throw newBusinessException(resolve(), EXECUTOR_BALANCE_HAS_NOT_BEEN_INCREMENTED);
                    }
                    return updateExecutorBalanceResponse;
                }));
    }

    public CompletableFuture<ru.craftysoft.orderingsystem.executor.proto.UpdateExecutorBalanceResponse> decreaseAmount(DecreaseExecutorAmountRequest decreaseExecutorAmountRequest) {
        var request = requestBuilder.build(decreaseExecutorAmountRequest);
        return client.updateExecutorBalance(request)
                .thenApply(withMdc((updateExecutorBalanceResponse) -> {
                    if (BALANCE_HAS_NOT_BEEN_CHANGED.equals(updateExecutorBalanceResponse.getUpdateExecutorBalanceResponseData().getResult())) {
                        throw newBusinessException(resolve(), EXECUTOR_BALANCE_HAS_NOT_BEEN_DECREASED);
                    }
                    return updateExecutorBalanceResponse;
                }));
    }
}
