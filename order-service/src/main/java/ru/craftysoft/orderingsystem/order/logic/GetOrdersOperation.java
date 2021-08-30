package ru.craftysoft.orderingsystem.order.logic;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.order.builder.operation.GetOrdersResponseBuilder;
import ru.craftysoft.orderingsystem.order.error.operation.ModuleOperationCode;
import ru.craftysoft.orderingsystem.order.proto.GetOrdersResponse;
import ru.craftysoft.orderingsystem.order.service.dao.OrderDaoAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.mapException;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

@Singleton
@Slf4j
public class GetOrdersOperation {

    private final OrderDaoAdapter orderDaoAdapter;
    private final GetOrdersResponseBuilder responseBuilder;

    @Inject
    public GetOrdersOperation(OrderDaoAdapter orderDaoAdapter, GetOrdersResponseBuilder responseBuilder) {
        this.orderDaoAdapter = orderDaoAdapter;
        this.responseBuilder = responseBuilder;
    }

    public CompletableFuture<GetOrdersResponse> process() {
        log.info("GetOrdersOperation.process.in");
        return orderDaoAdapter.getOrders()
                .handleAsync(withMdc((orders, throwable) -> {
                    if (throwable != null) {
                        log.error("GetOrdersOperation.process.thrown {}", throwable.getMessage());
                        throw mapException(throwable, ModuleOperationCode::resolve);
                    }
                    log.info("GetOrdersOperation.process.out");
                    return responseBuilder.build(orders);
                }));
    }
}
