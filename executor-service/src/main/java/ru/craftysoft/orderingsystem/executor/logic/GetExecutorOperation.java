package ru.craftysoft.orderingsystem.executor.logic;

import io.grpc.Context;
import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.executor.proto.GetExecutorRequest;
import ru.craftysoft.orderingsystem.executor.proto.GetExecutorResponse;
import ru.craftysoft.orderingsystem.executor.proto.GetExecutorResponseData;
import ru.craftysoft.orderingsystem.executor.service.dao.ExecutorDaoAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withContext;
import static ru.craftysoft.orderingsystem.util.proto.ProtoUtils.bigDecimalToMoney;

@Singleton
@Slf4j
public class GetExecutorOperation {

    private final ExecutorDaoAdapter executorDaoAdapter;

    @Inject
    public GetExecutorOperation(ExecutorDaoAdapter executorDaoAdapter) {
        this.executorDaoAdapter = executorDaoAdapter;
    }

    public CompletableFuture<GetExecutorResponse> process(GetExecutorRequest request) {
        log.info("GetExecutorOperation.process.in");
        var context = Context.current();
        return executorDaoAdapter.getExecutor(request)
                .handleAsync((executor, throwable) -> {
                    if (throwable != null) {
                        withContext(context, () -> log.error("GetExecutorOperation.process.thrown {}", throwable.getMessage()));
                        throw new RuntimeException(throwable);
                    }
                    withContext(context, () -> log.info("GetExecutorOperation.process.out"));
                    return GetExecutorResponse.newBuilder()
                            .setGetExecutorResponseData(GetExecutorResponseData.newBuilder()
                                    .setId(executor.id())
                                    .setBalance(bigDecimalToMoney(executor.balance()))
                            )
                            .build();
                });
    }
}
