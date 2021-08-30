package ru.craftysoft.orderingsystem.executor.builder.operation;

import ru.craftysoft.orderingsystem.executor.dto.Executor;
import ru.craftysoft.orderingsystem.executor.proto.GetExecutorResponse;
import ru.craftysoft.orderingsystem.executor.proto.GetExecutorResponseData;
import ru.craftysoft.orderingsystem.util.error.exception.BaseException;

import javax.inject.Inject;
import javax.inject.Singleton;

import static ru.craftysoft.orderingsystem.util.proto.ProtoUtils.bigDecimalToMoney;

@Singleton
public class GetExecutorResponseBuilder {

    private final ErrorBuilder errorBuilder;

    @Inject
    public GetExecutorResponseBuilder(ErrorBuilder errorBuilder) {
        this.errorBuilder = errorBuilder;
    }

    public GetExecutorResponse build(BaseException baseException) {
        return GetExecutorResponse.newBuilder()
                .setError(errorBuilder.build(baseException))
                .build();
    }

    public GetExecutorResponse build(Executor executor) {
        return GetExecutorResponse.newBuilder()
                .setGetExecutorResponseData(GetExecutorResponseData.newBuilder()
                        .setId(executor.id())
                        .setBalance(bigDecimalToMoney(executor.balance()))
                        .build()
                )
                .build();
    }
}
