package ru.craftysoft.orderingsystem.orderprocessing.builder.redis;

import lombok.SneakyThrows;
import ru.craftysoft.orderingsystem.orderprocessing.proto.*;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class IncrementCustomerAmountRequestBuilder {

    @Inject
    public IncrementCustomerAmountRequestBuilder() {
    }

    public IncrementCustomerAmountRequest build(IncrementCustomerAmountRequest request, int counter) {
        return IncrementCustomerAmountRequest.newBuilder(request)
                .setRetryData(RetryData.newBuilder()
                        .setCounter(counter)
                        .build())
                .build();
    }

    public IncrementCustomerAmountRequest build(IncrementExecutorAmountRequest incrementExecutorAmountRequest) {
        return IncrementCustomerAmountRequest.newBuilder()
                .setOrderId(incrementExecutorAmountRequest.getOrderId())
                .setExecutorId(incrementExecutorAmountRequest.getExecutorId())
                .setCustomerId(incrementExecutorAmountRequest.getCustomerId())
                .setAmount(incrementExecutorAmountRequest.getAmount())
                .build();
    }

    public IncrementCustomerAmountRequest build(DecreaseExecutorAmountRequest decreaseExecutorAmountRequest) {
        return IncrementCustomerAmountRequest.newBuilder()
                .setOrderId(decreaseExecutorAmountRequest.getOrderId())
                .setExecutorId(decreaseExecutorAmountRequest.getExecutorId())
                .setCustomerId(decreaseExecutorAmountRequest.getCustomerId())
                .setAmount(decreaseExecutorAmountRequest.getAmount())
                .build();
    }

    @SneakyThrows
    public IncrementCustomerAmountRequest fromBytes(byte[] bytes) {
        return IncrementCustomerAmountRequest.parseFrom(bytes);
    }
}
