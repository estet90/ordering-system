package ru.craftysoft.orderingsystem.gateway.logic;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.gateway.error.operation.ModuleOperationCode;
import ru.craftysoft.orderingsystem.gateway.order.rest.model.AddOrderRequestData;
import ru.craftysoft.orderingsystem.gateway.order.rest.model.AddOrderResponseData;
import ru.craftysoft.orderingsystem.gateway.service.grpc.CustomerServiceClientAdapter;
import ru.craftysoft.orderingsystem.gateway.service.grpc.OrderServiceClientAdapter;
import ru.craftysoft.orderingsystem.gateway.service.grpc.UserServiceClientAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.mapException;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

@Slf4j
@Singleton
public class AddOrderOperation {

    private final UserServiceClientAdapter userServiceClientAdapter;
    private final CustomerServiceClientAdapter customerServiceClientAdapter;
    private final OrderServiceClientAdapter orderServiceClientAdapter;

    @Inject
    public AddOrderOperation(UserServiceClientAdapter userServiceClientAdapter, CustomerServiceClientAdapter customerServiceClientAdapter, OrderServiceClientAdapter orderServiceClientAdapter) {
        this.userServiceClientAdapter = userServiceClientAdapter;
        this.customerServiceClientAdapter = customerServiceClientAdapter;
        this.orderServiceClientAdapter = orderServiceClientAdapter;
    }

    public CompletableFuture<AddOrderResponseData> process(String authorization, AddOrderRequestData addOrderRequestData) {
        log.info("AddOrderOperation.process.in");
        return userServiceClientAdapter.getUserId(authorization)
                .thenCompose(withMdc(customerServiceClientAdapter::getCustomer))
                .thenCompose(withMdc(getCustomerResponse -> {
                    return orderServiceClientAdapter.addOrder(addOrderRequestData, getCustomerResponse);
                }))
                .handleAsync(withMdc((addOrderResponse, throwable) -> {
                    if (throwable != null) {
                        log.error("AddOrderOperation.process.thrown {}", throwable.getMessage());
                        throw mapException(throwable, ModuleOperationCode::resolve);
                    }
                    var response = new AddOrderResponseData();
                    response.setId(addOrderResponse.getAddOrderResponseData().getId());
                    log.info("AddOrderOperation.process.out");
                    return response;
                }));
    }
}
