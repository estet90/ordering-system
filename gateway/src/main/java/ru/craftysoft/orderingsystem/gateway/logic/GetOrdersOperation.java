package ru.craftysoft.orderingsystem.gateway.logic;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.gateway.error.operation.ModuleOperationCode;
import ru.craftysoft.orderingsystem.gateway.order.rest.model.Order;
import ru.craftysoft.orderingsystem.gateway.service.grpc.OrderServiceClientAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.mapException;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;
import static ru.craftysoft.orderingsystem.util.proto.ProtoUtils.moneyToBigDecimal;

@Slf4j
@Singleton
public class GetOrdersOperation {

    private final OrderServiceClientAdapter orderServiceClientAdapter;

    @Inject
    public GetOrdersOperation(OrderServiceClientAdapter orderServiceClientAdapter) {
        this.orderServiceClientAdapter = orderServiceClientAdapter;
    }

    public CompletableFuture<List<Order>> process() {
        log.info("GetOrdersOperation.process.in");
        return orderServiceClientAdapter.getOrders().handleAsync(withMdc((getOrdersResponse, throwable) -> {
            if (throwable != null) {
                log.error("GetOrdersOperation.process.thrown {}", throwable.getMessage());
                throw mapException(throwable, ModuleOperationCode::resolve);
            }
            var response = getOrdersResponse.getGetOrdersResponseData().getOrdersList().stream()
                    .map(order -> {
                                var resultOrder = new Order();
                                resultOrder.setId(order.getId());
                                resultOrder.setName(order.getName());
                                resultOrder.setPrice(moneyToBigDecimal(order.getPrice()));
                                resultOrder.setCustomerId(order.getCustomerId());
                                return resultOrder;
                            }
                    )
                    .toList();
            log.info("GetOrdersOperation.process.out");
            return response;
        }));
    }

}
