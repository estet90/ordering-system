package ru.craftysoft.orderingsystem.executor.logic;

import io.grpc.Context;
import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.executor.proto.UpdateExecutorBalanceRequest;
import ru.craftysoft.orderingsystem.executor.proto.UpdateExecutorBalanceResponse;
import ru.craftysoft.orderingsystem.executor.proto.UpdateExecutorBalanceResponseData;
import ru.craftysoft.orderingsystem.executor.service.dao.ExecutorDaoAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.executor.proto.UpdateExecutorBalanceResponseData.Result.BALANCE_HAS_BEEN_CHANGED;
import static ru.craftysoft.orderingsystem.executor.proto.UpdateExecutorBalanceResponseData.Result.BALANCE_HAS_NOT_BEEN_CHANGED;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withContext;

@Singleton
@Slf4j
public class UpdateExecutorBalanceOperation {

    private final ExecutorDaoAdapter executorDaoAdapter;

    @Inject
    public UpdateExecutorBalanceOperation(ExecutorDaoAdapter executorDaoAdapter) {
        this.executorDaoAdapter = executorDaoAdapter;
    }

    public CompletableFuture<UpdateExecutorBalanceResponse> process(UpdateExecutorBalanceRequest request) {
        log.info("UpdateExecutorBalanceOperation.process.in");
        var context = Context.current();
        return executorDaoAdapter.updateExecutorBalance(request)
                .handleAsync((count, throwable) -> {
                    if (throwable != null) {
                        withContext(context, () -> log.error("UpdateExecutorBalanceOperation.process.thrown {}", throwable.getMessage()));
                        throw new RuntimeException(throwable);
                    }
                    withContext(context, () -> log.error("UpdateExecutorBalanceOperation.process.out"));
                    var result = count >= 1
                            ? BALANCE_HAS_BEEN_CHANGED
                            : BALANCE_HAS_NOT_BEEN_CHANGED;
                    return UpdateExecutorBalanceResponse.newBuilder()
                            .setUpdateExecutorBalanceResponseData(UpdateExecutorBalanceResponseData.newBuilder()
                                    .setResult(result)
                            )
                            .build();
                });
    }
}
