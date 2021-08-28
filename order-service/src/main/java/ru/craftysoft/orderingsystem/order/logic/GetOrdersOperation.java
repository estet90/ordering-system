package ru.craftysoft.orderingsystem.order.logic;

import io.grpc.Context;
import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.order.proto.GetOrdersRequest;
import ru.craftysoft.orderingsystem.order.proto.GetOrdersResponse;
import ru.craftysoft.orderingsystem.order.proto.GetOrdersResponseData;
import ru.craftysoft.orderingsystem.order.service.dao.OrderDaoAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withContext;
import static ru.craftysoft.orderingsystem.util.proto.ProtoUtils.bigDecimalToMoney;

@Singleton
@Slf4j
public class GetOrdersOperation {

    private final OrderDaoAdapter orderDaoAdapter;

    @Inject
    public GetOrdersOperation(OrderDaoAdapter orderDaoAdapter) {
        this.orderDaoAdapter = orderDaoAdapter;
    }

    public CompletableFuture<GetOrdersResponse> process(GetOrdersRequest request) {
        log.info("GetOrdersOperation.process.in");
        var context = Context.current();
        return orderDaoAdapter.getOrders()
                .handleAsync((orders, throwable) -> {
                    if (throwable != null) {
                        withContext(context, () -> log.error("GetRolesOperation.process.thrown {}", throwable.getMessage()));
                        throw new RuntimeException(throwable);
                    }
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
                });
    }
}
