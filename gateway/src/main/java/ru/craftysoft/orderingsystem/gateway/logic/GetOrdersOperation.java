package ru.craftysoft.orderingsystem.gateway.logic;

import ru.craftysoft.orderingsystem.gateway.order.rest.model.Order;
import ru.craftysoft.orderingsystem.gateway.service.grpc.OrderServiceClientAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.util.proto.ProtoUtils.moneyToBigDecimal;

@Singleton
public class GetOrdersOperation {

    private final OrderServiceClientAdapter orderServiceClientAdapter;

    @Inject
    public GetOrdersOperation(OrderServiceClientAdapter orderServiceClientAdapter) {
        this.orderServiceClientAdapter = orderServiceClientAdapter;
    }

    public CompletableFuture<List<Order>> process() {
        return orderServiceClientAdapter.getOrders().handleAsync((getOrdersResponse, throwable) -> {
            if (throwable != null) {
                throw new RuntimeException(throwable);
            }
            return getOrdersResponse.getGetOrdersResponseData().getOrdersList().stream()
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
        });
    }

}
