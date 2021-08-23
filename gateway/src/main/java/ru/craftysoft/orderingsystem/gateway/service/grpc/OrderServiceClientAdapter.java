package ru.craftysoft.orderingsystem.gateway.service.grpc;

import ru.craftysoft.orderingsystem.order.proto.GetOrdersRequest;
import ru.craftysoft.orderingsystem.order.proto.GetOrdersResponse;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

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
}
