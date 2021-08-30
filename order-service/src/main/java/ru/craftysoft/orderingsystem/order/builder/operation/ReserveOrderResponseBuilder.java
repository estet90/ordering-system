package ru.craftysoft.orderingsystem.order.builder.operation;

import ru.craftysoft.orderingsystem.order.proto.ReserveOrderResponse;
import ru.craftysoft.orderingsystem.order.proto.ReserveOrderResponseData;
import ru.craftysoft.orderingsystem.util.error.exception.BaseException;

import javax.inject.Inject;
import javax.inject.Singleton;

import static ru.craftysoft.orderingsystem.order.proto.ReserveOrderResponseData.Result.FAILED_TO_RESERVE;
import static ru.craftysoft.orderingsystem.order.proto.ReserveOrderResponseData.Result.RESERVED;

@Singleton
public class ReserveOrderResponseBuilder {

    private final ErrorBuilder errorBuilder;

    @Inject
    public ReserveOrderResponseBuilder(ErrorBuilder errorBuilder) {
        this.errorBuilder = errorBuilder;
    }

    public ReserveOrderResponse build(BaseException baseException) {
        var error = errorBuilder.build(baseException);
        return ReserveOrderResponse.newBuilder()
                .setError(error)
                .build();
    }

    public ReserveOrderResponse build(Integer count) {
        var result = count == 1
                ? RESERVED
                : FAILED_TO_RESERVE;
        return ReserveOrderResponse.newBuilder()
                .setReserveOrderResponseData(ReserveOrderResponseData.newBuilder().setResult(result))
                .build();
    }
}
