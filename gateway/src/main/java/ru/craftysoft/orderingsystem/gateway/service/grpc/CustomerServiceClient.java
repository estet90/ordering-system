package ru.craftysoft.orderingsystem.gateway.service.grpc;

import io.grpc.stub.StreamObserver;
import ru.craftysoft.orderingsystem.customer.proto.CustomerServiceGrpc;
import ru.craftysoft.orderingsystem.customer.proto.GetCustomerRequest;
import ru.craftysoft.orderingsystem.customer.proto.GetCustomerResponse;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class CustomerServiceClient {

    private final CustomerServiceGrpc.CustomerServiceStub customerServiceStub;

    @Inject
    public CustomerServiceClient(CustomerServiceGrpc.CustomerServiceStub customerServiceStub) {
        this.customerServiceStub = customerServiceStub;
    }

    public CompletableFuture<GetCustomerResponse> getCustomer(GetCustomerRequest request) {
        var result = new CompletableFuture<GetCustomerResponse>();
        customerServiceStub.getCustomer(request, new StreamObserver<>() {
            private GetCustomerResponse response;

            @Override
            public void onNext(GetCustomerResponse response) {
                this.response = response;
            }

            @Override
            public void onError(Throwable throwable) {
                result.completeExceptionally(throwable);
            }

            @Override
            public void onCompleted() {
                result.complete(this.response);
            }
        });
        return result;
    }
}
