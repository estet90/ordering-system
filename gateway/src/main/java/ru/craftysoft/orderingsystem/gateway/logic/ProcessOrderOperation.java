package ru.craftysoft.orderingsystem.gateway.logic;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.gateway.error.operation.ModuleOperationCode;
import ru.craftysoft.orderingsystem.gateway.order.rest.model.ProcessOrderResponseData;
import ru.craftysoft.orderingsystem.gateway.service.grpc.ExecutorServiceClientAdapter;
import ru.craftysoft.orderingsystem.gateway.service.grpc.OrderServiceClientAdapter;
import ru.craftysoft.orderingsystem.gateway.service.grpc.UserServiceClientAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.order.proto.ReserveOrderResponseData.Result.RESERVED;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.mapException;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

@Slf4j
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
        log.info("ProcessOrderOperation.process.in");
        return userServiceClientAdapter.getUserId(authorization)
                .thenCompose(withMdc(executorServiceClientAdapter::getExecutor))
                .thenCompose(withMdc(getUserIdResponse -> {
                    return orderServiceClientAdapter.reserveOrder(id, getUserIdResponse);
                }))
                .handleAsync(withMdc((reserveOrderResponse, throwable) -> {
                    if (throwable != null) {
                        log.error("ProcessOrderOperation.process.thrown {}", throwable.getMessage());
                        throw mapException(throwable, ModuleOperationCode::resolve);
                    }
                    var message = RESERVED.equals(reserveOrderResponse.getReserveOrderResponseData().getResult())
                            ? "Заказ зарезервирован"
                            : "Не удалось зарезервировать заказ";
                    var response = new ProcessOrderResponseData();
                    response.setMessage(message);
                    log.info("ProcessOrderOperation.process.in");
                    return response;
                }));
    }
}
