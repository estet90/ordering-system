package ru.craftysoft.orderingsystem.orderprocessing.builder.redis;

import lombok.SneakyThrows;
import ru.craftysoft.orderingsystem.customer.proto.UpdateCustomerBalanceResponse;
import ru.craftysoft.orderingsystem.orderprocessing.proto.DecreaseCustomerAmountRequest;
import ru.craftysoft.orderingsystem.orderprocessing.proto.IncrementExecutorAmountRequest;
import ru.craftysoft.orderingsystem.orderprocessing.proto.RetryData;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class IncrementExecutorAmountRequestBuilder {

    @Inject
    public IncrementExecutorAmountRequestBuilder() {
    }

    public IncrementExecutorAmountRequest build(UpdateCustomerBalanceResponse updateCustomerBalanceResponse, DecreaseCustomerAmountRequest decreaseCustomerAmountRequest) {
        return IncrementExecutorAmountRequest.newBuilder()
                .setCustomerId(decreaseCustomerAmountRequest.getCustomerId())
                .setExecutorId(decreaseCustomerAmountRequest.getExecutorId())
                .setOrderId(decreaseCustomerAmountRequest.getOrderId())
                .setAmount(decreaseCustomerAmountRequest.getAmount())
                .setCustomerBalance(updateCustomerBalanceResponse.getUpdateCustomerBalanceResponseData().getBalance())
                .build();
    }

    public IncrementExecutorAmountRequest build(IncrementExecutorAmountRequest request, int counter) {
        return IncrementExecutorAmountRequest.newBuilder(request)
                .setRetryData(RetryData.newBuilder()
                        .setCounter(counter)
                        .build())
                .build();
    }

    @SneakyThrows
    public IncrementExecutorAmountRequest fromBytes(byte[] bytes) {
        return IncrementExecutorAmountRequest.parseFrom(bytes);
    }
}
