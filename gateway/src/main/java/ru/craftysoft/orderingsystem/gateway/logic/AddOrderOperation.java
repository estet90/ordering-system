package ru.craftysoft.orderingsystem.gateway.logic;

import ru.craftysoft.orderingsystem.gateway.order.rest.model.AddOrderRequestData;
import ru.craftysoft.orderingsystem.gateway.order.rest.model.AddOrderResponseData;
import ru.craftysoft.orderingsystem.gateway.service.grpc.CustomerServiceClientAdapter;
import ru.craftysoft.orderingsystem.gateway.service.grpc.OrderServiceClientAdapter;
import ru.craftysoft.orderingsystem.gateway.service.grpc.UserServiceClientAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

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
        return userServiceClientAdapter.getUserId(authorization)
                .thenCompose(customerServiceClientAdapter::getCustomer)
                .thenCompose(getCustomerResponse -> orderServiceClientAdapter.addOrder(addOrderRequestData, getCustomerResponse))
                .handleAsync((addOrderResponse, throwable) -> {
                    if (throwable != null) {
                        throw new RuntimeException(throwable);
                    }
                    var response = new AddOrderResponseData();
                    response.setId(addOrderResponse.getAddOrderResponseData().getId());
                    return response;
                });
    }
}
