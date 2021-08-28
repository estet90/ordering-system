package ru.craftysoft.orderingsystem.orderprocessing.service.grpc;

import io.grpc.stub.StreamObserver;
import ru.craftysoft.orderingsystem.customer.proto.CustomerServiceGrpc;
import ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceRequest;
import ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceResponse;

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

    public CompletableFuture<UpdateCustomerBalanceResponse> updateCustomerBalance(UpdateCustomerBalanceRequest request) {
        var result = new CompletableFuture<UpdateCustomerBalanceResponse>();
        customerServiceStub.updateCustomerBalance(request, new StreamObserver<>() {
            private UpdateCustomerBalanceResponse response;

            @Override
            public void onNext(UpdateCustomerBalanceResponse response) {
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
