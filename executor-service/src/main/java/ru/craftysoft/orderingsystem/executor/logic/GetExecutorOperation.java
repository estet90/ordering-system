package ru.craftysoft.orderingsystem.executor.logic;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.executor.builder.operation.GetExecutorResponseBuilder;
import ru.craftysoft.orderingsystem.executor.error.operation.ModuleOperationCode;
import ru.craftysoft.orderingsystem.executor.proto.GetExecutorRequest;
import ru.craftysoft.orderingsystem.executor.proto.GetExecutorResponse;
import ru.craftysoft.orderingsystem.executor.service.dao.ExecutorDaoAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.mapException;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

@Singleton
@Slf4j
public class GetExecutorOperation {

    private final ExecutorDaoAdapter executorDaoAdapter;
    private final GetExecutorResponseBuilder responseBuilder;

    @Inject
    public GetExecutorOperation(ExecutorDaoAdapter executorDaoAdapter, GetExecutorResponseBuilder responseBuilder) {
        this.executorDaoAdapter = executorDaoAdapter;
        this.responseBuilder = responseBuilder;
    }

    public CompletableFuture<GetExecutorResponse> process(GetExecutorRequest request) {
        log.info("GetExecutorOperation.process.in");
        return executorDaoAdapter.getExecutor(request)
                .handleAsync(withMdc((executor, throwable) -> {
                    if (throwable != null) {
                        log.error("GetExecutorOperation.process.thrown {}", throwable.getMessage());
                        throw mapException(throwable, ModuleOperationCode::resolve);
                    }
                    log.info("GetExecutorOperation.process.out");
                    return responseBuilder.build(executor);
                }));
    }
}
