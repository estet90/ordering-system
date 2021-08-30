package ru.craftysoft.orderingsystem.executor.logic;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.executor.builder.operation.UpdateExecutorBalanceResponseBuilder;
import ru.craftysoft.orderingsystem.executor.error.operation.ModuleOperationCode;
import ru.craftysoft.orderingsystem.executor.proto.UpdateExecutorBalanceRequest;
import ru.craftysoft.orderingsystem.executor.proto.UpdateExecutorBalanceResponse;
import ru.craftysoft.orderingsystem.executor.service.dao.ExecutorDaoAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.mapException;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

@Singleton
@Slf4j
public class UpdateExecutorBalanceOperation {

    private final ExecutorDaoAdapter executorDaoAdapter;
    private final UpdateExecutorBalanceResponseBuilder responseBuilder;

    @Inject
    public UpdateExecutorBalanceOperation(ExecutorDaoAdapter executorDaoAdapter, UpdateExecutorBalanceResponseBuilder responseBuilder) {
        this.executorDaoAdapter = executorDaoAdapter;
        this.responseBuilder = responseBuilder;
    }

    public CompletableFuture<UpdateExecutorBalanceResponse> process(UpdateExecutorBalanceRequest request) {
        log.info("UpdateExecutorBalanceOperation.process.in");
        return executorDaoAdapter.updateExecutorBalance(request)
                .handleAsync(withMdc((count, throwable) -> {
                    if (throwable != null) {
                        log.error("UpdateExecutorBalanceOperation.process.thrown {}", throwable.getMessage());
                        throw mapException(throwable, ModuleOperationCode::resolve);
                    }
                    log.error("UpdateExecutorBalanceOperation.process.out");
                    return responseBuilder.build(count);
                }));
    }
}
