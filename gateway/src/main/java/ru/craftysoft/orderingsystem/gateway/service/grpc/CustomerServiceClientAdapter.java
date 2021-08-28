package ru.craftysoft.orderingsystem.gateway.service.grpc;

import ru.craftysoft.orderingsystem.customer.proto.GetCustomerRequest;
import ru.craftysoft.orderingsystem.customer.proto.GetCustomerResponse;
import ru.craftysoft.orderingsystem.user.proto.GetUserIdResponse;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class CustomerServiceClientAdapter {

    private final CustomerServiceClient client;

    @Inject
    public CustomerServiceClientAdapter(CustomerServiceClient client) {
        this.client = client;
    }

    public CompletableFuture<GetCustomerResponse> getCustomer(GetUserIdResponse getUserIdResponse) {
        var request = GetCustomerRequest.newBuilder()
                .setUserId(getUserIdResponse.getGetUserResponseData().getId())
                .build();
        return client.getCustomer(request);
    }
}
