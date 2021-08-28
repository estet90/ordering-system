package ru.craftysoft.orderingsystem.orderprocessing.builder.redis;

import lombok.SneakyThrows;
import ru.craftysoft.orderingsystem.orderprocessing.proto.CompleteOrderRequest;
import ru.craftysoft.orderingsystem.orderprocessing.proto.IncrementExecutorAmountRequest;
import ru.craftysoft.orderingsystem.orderprocessing.proto.RetryData;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CompleteOrderRequestBuilder {

    @Inject
    public CompleteOrderRequestBuilder() {
    }

    public CompleteOrderRequest build(CompleteOrderRequest request, int counter) {
        return CompleteOrderRequest.newBuilder(request)
                .setRetryData(RetryData.newBuilder()
                        .setCounter(counter)
                        .build())
                .build();
    }

    public CompleteOrderRequest build(IncrementExecutorAmountRequest incrementExecutorAmountRequest) {
        return CompleteOrderRequest.newBuilder()
                .setOrderId(incrementExecutorAmountRequest.getOrderId())
                .setCustomerId(incrementExecutorAmountRequest.getCustomerId())
                .setExecutorId(incrementExecutorAmountRequest.getExecutorId())
                .setAmount(incrementExecutorAmountRequest.getAmount())
                .setCustomerBalance(incrementExecutorAmountRequest.getCustomerBalance())
                .build();
    }

    @SneakyThrows
    public CompleteOrderRequest fromBytes(byte[] bytes) {
        return CompleteOrderRequest.parseFrom(bytes);
    }
}
