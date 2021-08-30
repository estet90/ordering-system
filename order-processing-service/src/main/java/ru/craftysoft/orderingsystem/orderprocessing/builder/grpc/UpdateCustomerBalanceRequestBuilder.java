package ru.craftysoft.orderingsystem.orderprocessing.builder.grpc;

import ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceRequest;
import ru.craftysoft.orderingsystem.orderprocessing.proto.DecreaseCustomerAmountRequest;
import ru.craftysoft.orderingsystem.orderprocessing.proto.IncrementCustomerAmountRequest;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UpdateCustomerBalanceRequestBuilder {

    @Inject
    public UpdateCustomerBalanceRequestBuilder() {
    }

    public UpdateCustomerBalanceRequest build(DecreaseCustomerAmountRequest decreaseCustomerAmountRequest) {
        return UpdateCustomerBalanceRequest.newBuilder()
                .setId(decreaseCustomerAmountRequest.getCustomerId())
                .setDecreaseAmount(decreaseCustomerAmountRequest.getAmount())
                .build();
    }

    public UpdateCustomerBalanceRequest build(IncrementCustomerAmountRequest incrementCustomerAmountRequest) {
        return UpdateCustomerBalanceRequest.newBuilder()
                .setId(incrementCustomerAmountRequest.getCustomerId())
                .setIncrementAmount(incrementCustomerAmountRequest.getAmount())
                .build();
    }
}
