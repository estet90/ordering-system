package ru.craftysoft.orderingsystem.orderprocessing.builder.redis;

import lombok.SneakyThrows;
import ru.craftysoft.orderingsystem.orderprocessing.proto.CompleteOrderRequest;
import ru.craftysoft.orderingsystem.orderprocessing.proto.DecreaseExecutorAmountRequest;
import ru.craftysoft.orderingsystem.orderprocessing.proto.RetryData;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DecreaseExecutorAmountRequestBuilder {

    @Inject
    public DecreaseExecutorAmountRequestBuilder() {
    }

    @SneakyThrows
    public DecreaseExecutorAmountRequest fromBytes(byte[] bytes) {
        return DecreaseExecutorAmountRequest.parseFrom(bytes);
    }

    public DecreaseExecutorAmountRequest build(CompleteOrderRequest completeOrderRequest) {
        return DecreaseExecutorAmountRequest.newBuilder()
                .setOrderId(completeOrderRequest.getOrderId())
                .setCustomerId(completeOrderRequest.getCustomerId())
                .setExecutorId(completeOrderRequest.getExecutorId())
                .setAmount(completeOrderRequest.getAmount())
                .build();
    }

    public DecreaseExecutorAmountRequest build(DecreaseExecutorAmountRequest request, int counter) {
        return DecreaseExecutorAmountRequest.newBuilder(request)
                .setRetryData(RetryData.newBuilder()
                        .setCounter(counter)
                        .build())
                .build();
    }
}
