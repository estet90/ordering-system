package ru.craftysoft.orderingsystem.gateway.service.grpc;

import ru.craftysoft.orderingsystem.customer.proto.GetCustomerResponse;
import ru.craftysoft.orderingsystem.executor.proto.GetExecutorResponse;
import ru.craftysoft.orderingsystem.gateway.order.rest.model.AddOrderRequestData;
import ru.craftysoft.orderingsystem.order.proto.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.util.proto.ProtoUtils.bigDecimalToMoney;

@Singleton
public class OrderServiceClientAdapter {

    private final OrderServiceClient client;

    @Inject
    public OrderServiceClientAdapter(OrderServiceClient client) {
        this.client = client;
    }

    public CompletableFuture<GetOrdersResponse> getOrders() {
        var request = GetOrdersRequest.newBuilder().build();
        return client.getOrders(request);
    }

    public CompletableFuture<AddOrderResponse> addOrder(AddOrderRequestData addOrderRequestData, GetCustomerResponse getCustomerResponse) {
        var request = AddOrderRequest.newBuilder()
                .setName(addOrderRequestData.getName())
                .setPrice(bigDecimalToMoney(addOrderRequestData.getPrice()))
                .setCustomer(AddOrderRequest.Customer.newBuilder()
                        .setId(getCustomerResponse.getGetCustomerResponseData().getId())
                        .setBalance(getCustomerResponse.getGetCustomerResponseData().getBalance())
                        .build())
                .build();
        return client.addOrder(request);
    }

    public CompletableFuture<ReserveOrderResponse> reserveOrder(long orderId, GetExecutorResponse getExecutorResponse) {
        var request = ReserveOrderRequest.newBuilder()
                .setId(orderId)
                .setExecutorId(getExecutorResponse.getGetExecutorResponseData().getId())
                .build();
        return client.reserveOrder(request);
    }
}
