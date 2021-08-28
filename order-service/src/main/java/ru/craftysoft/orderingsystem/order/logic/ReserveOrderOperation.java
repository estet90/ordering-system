package ru.craftysoft.orderingsystem.order.logic;

import io.grpc.Context;
import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.order.proto.*;
import ru.craftysoft.orderingsystem.order.service.dao.OrderDaoAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.order.proto.ReserveOrderResponseData.Result.FAILED_TO_RESERVE;
import static ru.craftysoft.orderingsystem.order.proto.ReserveOrderResponseData.Result.RESERVED;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withContext;

@Singleton
@Slf4j
public class ReserveOrderOperation {

    private final OrderDaoAdapter orderDaoAdapter;

    @Inject
    public ReserveOrderOperation(OrderDaoAdapter orderDaoAdapter) {
        this.orderDaoAdapter = orderDaoAdapter;
    }

    public CompletableFuture<ReserveOrderResponse> process(ReserveOrderRequest request) {
        log.info("ReserveOrderOperation.process.in");
        var context = Context.current();
        return orderDaoAdapter.reserveOrder(request)
                .handleAsync((count, throwable) -> {
                    if (throwable != null) {
                        withContext(context, () -> log.error("ReserveOrderOperation.process.thrown {}", throwable.getMessage()));
                        throw new RuntimeException(throwable);
                    }
                    withContext(context, () -> log.error("ReserveOrderOperation.process.out"));
                    var result = count == 1
                            ? RESERVED
                            : FAILED_TO_RESERVE;
                    return ReserveOrderResponse.newBuilder()
                            .setReserveOrderResponseData(ReserveOrderResponseData.newBuilder().setResult(result))
                            .build();
                });
    }
}
