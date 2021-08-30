package ru.craftysoft.orderingsystem.order.logic;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.order.builder.operation.ReserveOrderResponseBuilder;
import ru.craftysoft.orderingsystem.order.error.operation.ModuleOperationCode;
import ru.craftysoft.orderingsystem.order.proto.ReserveOrderRequest;
import ru.craftysoft.orderingsystem.order.proto.ReserveOrderResponse;
import ru.craftysoft.orderingsystem.order.service.dao.OrderDaoAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.mapException;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

@Singleton
@Slf4j
public class ReserveOrderOperation {

    private final OrderDaoAdapter orderDaoAdapter;
    private final ReserveOrderResponseBuilder responseBuilder;

    @Inject
    public ReserveOrderOperation(OrderDaoAdapter orderDaoAdapter, ReserveOrderResponseBuilder responseBuilder) {
        this.orderDaoAdapter = orderDaoAdapter;
        this.responseBuilder = responseBuilder;
    }

    public CompletableFuture<ReserveOrderResponse> process(ReserveOrderRequest request) {
        log.info("ReserveOrderOperation.process.in");
        return orderDaoAdapter.reserveOrder(request)
                .handleAsync(withMdc((count, throwable) -> {
                    if (throwable != null) {
                        log.error("ReserveOrderOperation.process.thrown {}", throwable.getMessage());
                        throw mapException(throwable, ModuleOperationCode::resolve);
                    }
                    log.error("ReserveOrderOperation.process.out");
                    return responseBuilder.build(count);
                }));
    }
}
