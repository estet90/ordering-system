package ru.craftysoft.orderingsystem.executor.builder.operation;

import ru.craftysoft.orderingsystem.executor.proto.UpdateExecutorBalanceResponse;
import ru.craftysoft.orderingsystem.executor.proto.UpdateExecutorBalanceResponseData;
import ru.craftysoft.orderingsystem.util.error.exception.BaseException;

import javax.inject.Inject;
import javax.inject.Singleton;

import static ru.craftysoft.orderingsystem.executor.proto.UpdateExecutorBalanceResponseData.Result.BALANCE_HAS_BEEN_CHANGED;
import static ru.craftysoft.orderingsystem.executor.proto.UpdateExecutorBalanceResponseData.Result.BALANCE_HAS_NOT_BEEN_CHANGED;

@Singleton
public class UpdateExecutorBalanceResponseBuilder {

    private final ErrorBuilder errorBuilder;

    @Inject
    public UpdateExecutorBalanceResponseBuilder(ErrorBuilder errorBuilder) {
        this.errorBuilder = errorBuilder;
    }

    public UpdateExecutorBalanceResponse build(BaseException baseException) {
        return UpdateExecutorBalanceResponse.newBuilder()
                .setError(errorBuilder.build(baseException))
                .build();
    }

    public UpdateExecutorBalanceResponse build(Integer count) {
        var result = count == 1
                ? BALANCE_HAS_BEEN_CHANGED
                : BALANCE_HAS_NOT_BEEN_CHANGED;
        return UpdateExecutorBalanceResponse.newBuilder()
                .setUpdateExecutorBalanceResponseData(UpdateExecutorBalanceResponseData.newBuilder()
                        .setResult(result)
                        .build()
                )
                .build();
    }
}
