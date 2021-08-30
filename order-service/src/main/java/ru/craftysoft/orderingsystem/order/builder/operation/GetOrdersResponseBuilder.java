package ru.craftysoft.orderingsystem.order.builder.operation;

import ru.craftysoft.orderingsystem.order.dto.Order;
import ru.craftysoft.orderingsystem.order.proto.GetOrdersResponse;
import ru.craftysoft.orderingsystem.order.proto.GetOrdersResponseData;
import ru.craftysoft.orderingsystem.util.error.exception.BaseException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static ru.craftysoft.orderingsystem.util.proto.ProtoUtils.bigDecimalToMoney;

@Singleton
public class GetOrdersResponseBuilder {

    private final ErrorBuilder errorBuilder;

    @Inject
    public GetOrdersResponseBuilder(ErrorBuilder errorBuilder) {
        this.errorBuilder = errorBuilder;
    }

    public GetOrdersResponse build(BaseException baseException) {
        var error = errorBuilder.build(baseException);
        return GetOrdersResponse.newBuilder()
                .setError(error)
                .build();
    }

    public GetOrdersResponse build(List<Order> orders) {
        return GetOrdersResponse.newBuilder()
                .setGetOrdersResponseData(GetOrdersResponseData.newBuilder()
                        .addAllOrders(orders.stream()
                                .map(order -> GetOrdersResponseData.Order.newBuilder()
                                        .setId(order.id())
                                        .setName(order.name())
                                        .setPrice(bigDecimalToMoney(order.price()))
                                        .setCustomerId(order.customerId())
                                        .build()
                                )
                                .toList()
                        )
                )
                .build();
    }
}
