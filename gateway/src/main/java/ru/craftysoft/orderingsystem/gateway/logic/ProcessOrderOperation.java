package ru.craftysoft.orderingsystem.gateway.logic;

import ru.craftysoft.orderingsystem.gateway.order.rest.model.ProcessOrderResponseData;
import ru.craftysoft.orderingsystem.gateway.service.grpc.ExecutorServiceClientAdapter;
import ru.craftysoft.orderingsystem.gateway.service.grpc.OrderServiceClientAdapter;
import ru.craftysoft.orderingsystem.gateway.service.grpc.UserServiceClientAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.order.proto.ReserveOrderResponseData.Result.RESERVED;

@Singleton
public class ProcessOrderOperation {

    private final UserServiceClientAdapter userServiceClientAdapter;
    private final ExecutorServiceClientAdapter executorServiceClientAdapter;
    private final OrderServiceClientAdapter orderServiceClientAdapter;

    @Inject
    public ProcessOrderOperation(UserServiceClientAdapter userServiceClientAdapter,
                                 ExecutorServiceClientAdapter executorServiceClientAdapter,
                                 OrderServiceClientAdapter orderServiceClientAdapter) {
        this.userServiceClientAdapter = userServiceClientAdapter;
        this.executorServiceClientAdapter = executorServiceClientAdapter;
        this.orderServiceClientAdapter = orderServiceClientAdapter;
    }

    public CompletableFuture<ProcessOrderResponseData> process(long id, String authorization) {
        return userServiceClientAdapter.getUserId(authorization)
                .thenCompose(executorServiceClientAdapter::getExecutor)
                .thenCompose(getUserIdResponse -> orderServiceClientAdapter.reserveOrder(id, getUserIdResponse))
                .handleAsync((reserveOrderResponse, throwable) -> {
                    if (throwable != null) {
                        throw new RuntimeException(throwable);
                    }
                    var message = RESERVED.equals(reserveOrderResponse.getReserveOrderResponseData().getResult())
                            ? "Заказ зарезервирован"
                            : "Не удалось зарезервировать заказ";
                    var response = new ProcessOrderResponseData();
                    response.setMessage(message);
                    return response;
                });
    }
}
