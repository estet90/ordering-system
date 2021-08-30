package ru.craftysoft.orderingsystem.order.logic;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.order.builder.operation.AddOrderResponseBuilder;
import ru.craftysoft.orderingsystem.order.error.operation.ModuleOperationCode;
import ru.craftysoft.orderingsystem.order.proto.AddOrderRequest;
import ru.craftysoft.orderingsystem.order.proto.AddOrderResponse;
import ru.craftysoft.orderingsystem.order.service.dao.OrderDaoAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.mapException;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

@Singleton
@Slf4j
public class AddOrderOperation {

    private final OrderDaoAdapter orderDaoAdapter;
    private final AddOrderResponseBuilder responseBuilder;

    @Inject
    public AddOrderOperation(OrderDaoAdapter orderDaoAdapter, AddOrderResponseBuilder responseBuilder) {
        this.orderDaoAdapter = orderDaoAdapter;
        this.responseBuilder = responseBuilder;
    }

    public CompletableFuture<AddOrderResponse> process(AddOrderRequest request) {
        log.info("AddOrderOperation.process.in");
        return orderDaoAdapter.addOrder(request)
                .handleAsync(withMdc((id, throwable) -> {
                    if (throwable != null) {
                        log.error("AddOrderOperation.process.thrown {}", throwable.getMessage());
                        throw mapException(throwable, ModuleOperationCode::resolve);
                    }
                    log.error("AddOrderOperation.process.out");
                    return responseBuilder.build(id);
                }));
    }
}
