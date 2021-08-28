package ru.craftysoft.orderingsystem.order.logic;

import io.grpc.Context;
import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.order.proto.AddOrderRequest;
import ru.craftysoft.orderingsystem.order.proto.AddOrderResponse;
import ru.craftysoft.orderingsystem.order.proto.AddOrderResponseData;
import ru.craftysoft.orderingsystem.order.service.dao.OrderDaoAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withContext;

@Singleton
@Slf4j
public class AddOrderOperation {

    private final OrderDaoAdapter orderDaoAdapter;

    @Inject
    public AddOrderOperation(OrderDaoAdapter orderDaoAdapter) {
        this.orderDaoAdapter = orderDaoAdapter;
    }

    public CompletableFuture<AddOrderResponse> process(AddOrderRequest request) {
        log.info("AddOrderOperation.process.in");
        var context = Context.current();
        return orderDaoAdapter.addOrder(request)
                .handleAsync((id, throwable) -> {
                    if (throwable != null) {
                        withContext(context, () -> log.error("GetRolesOperation.process.thrown {}", throwable.getMessage()));
                        throw new RuntimeException(throwable);
                    }
                    withContext(context, () -> log.error("GetRolesOperation.process.out"));
                    return AddOrderResponse.newBuilder()
                            .setAddOrderResponseData(AddOrderResponseData.newBuilder().setId(id))
                            .build();
                });
    }
}
