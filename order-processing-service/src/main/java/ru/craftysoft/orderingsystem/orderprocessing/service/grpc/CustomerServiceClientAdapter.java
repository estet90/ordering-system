package ru.craftysoft.orderingsystem.orderprocessing.service.grpc;

import ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceRequest;
import ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceResponse;
import ru.craftysoft.orderingsystem.orderprocessing.proto.DecreaseCustomerAmountRequest;
import ru.craftysoft.orderingsystem.orderprocessing.proto.IncrementCustomerAmountRequest;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceResponseData.Result.BALANCE_HAS_NOT_BEEN_CHANGED;

@Singleton
public class CustomerServiceClientAdapter {

    private final CustomerServiceClient client;

    @Inject
    public CustomerServiceClientAdapter(CustomerServiceClient client) {
        this.client = client;
    }

    public CompletableFuture<UpdateCustomerBalanceResponse> decreaseAmount(DecreaseCustomerAmountRequest decreaseCustomerAmountRequest) {
        var request = UpdateCustomerBalanceRequest.newBuilder()
                .setId(decreaseCustomerAmountRequest.getCustomerId())
                .setDecreaseAmount(decreaseCustomerAmountRequest.getAmount())
                .build();
        return client.updateCustomerBalance(request)
                .whenComplete((updateCustomerBalanceResponse, throwable) -> {
                    if (throwable != null) {
                        throw new RuntimeException(throwable);
                    }
                    if (BALANCE_HAS_NOT_BEEN_CHANGED.equals(updateCustomerBalanceResponse.getUpdateCustomerBalanceResponseData().getResult())) {
                        throw new RuntimeException();
                    }
                });
    }

    public CompletableFuture<UpdateCustomerBalanceResponse> incrementAmount(IncrementCustomerAmountRequest incrementCustomerAmountRequest) {
        var request = UpdateCustomerBalanceRequest.newBuilder()
                .setId(incrementCustomerAmountRequest.getCustomerId())
                .setIncrementAmount(incrementCustomerAmountRequest.getAmount())
                .build();
        return client.updateCustomerBalance(request)
                .whenComplete((updateCustomerBalanceResponse, throwable) -> {
                    if (throwable != null) {
                        throw new RuntimeException(throwable);
                    }
                    if (BALANCE_HAS_NOT_BEEN_CHANGED.equals(updateCustomerBalanceResponse.getUpdateCustomerBalanceResponseData().getResult())) {
                        throw new RuntimeException();
                    }
                });
    }
}
