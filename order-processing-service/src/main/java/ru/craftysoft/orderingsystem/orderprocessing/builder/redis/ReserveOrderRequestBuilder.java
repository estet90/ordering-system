package ru.craftysoft.orderingsystem.orderprocessing.builder.redis;

import lombok.SneakyThrows;
import ru.craftysoft.orderingsystem.orderprocessing.proto.DecreaseCustomerAmountRequest;
import ru.craftysoft.orderingsystem.orderprocessing.proto.IncrementCustomerAmountRequest;
import ru.craftysoft.orderingsystem.orderprocessing.proto.ReserveOrderRequest;
import ru.craftysoft.orderingsystem.orderprocessing.proto.RetryData;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ReserveOrderRequestBuilder {

    @Inject
    public ReserveOrderRequestBuilder() {
    }

    @SneakyThrows
    public ReserveOrderRequest fromBytes(byte[] bytes) {
        return ReserveOrderRequest.parseFrom(bytes);
    }

    public ReserveOrderRequest build(ReserveOrderRequest request, int counter) {
        return ReserveOrderRequest.newBuilder(request)
                .setRetryData(RetryData.newBuilder()
                        .setCounter(counter)
                        .build())
                .build();
    }

    public ReserveOrderRequest build(DecreaseCustomerAmountRequest decreaseCustomerAmountRequest) {
        return ReserveOrderRequest.newBuilder()
                .setOrderId(decreaseCustomerAmountRequest.getOrderId())
                .build();
    }

    public ReserveOrderRequest build(IncrementCustomerAmountRequest incrementCustomerAmountRequest) {
        return ReserveOrderRequest.newBuilder()
                .setOrderId(incrementCustomerAmountRequest.getOrderId())
                .build();
    }
}
