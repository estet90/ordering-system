package ru.craftysoft.orderingsystem.orderprocessing.builder.redis;

import lombok.SneakyThrows;
import ru.craftysoft.orderingsystem.orderprocessing.dto.Order;
import ru.craftysoft.orderingsystem.orderprocessing.proto.DecreaseCustomerAmountRequest;
import ru.craftysoft.orderingsystem.orderprocessing.proto.RetryData;

import javax.inject.Inject;
import javax.inject.Singleton;

import static ru.craftysoft.orderingsystem.util.proto.ProtoUtils.bigDecimalToMoney;

@Singleton
public class DecreaseCustomerAmountRequestBuilder {

    @Inject
    public DecreaseCustomerAmountRequestBuilder() {
    }

    public DecreaseCustomerAmountRequest build(Order order) {
        return DecreaseCustomerAmountRequest.newBuilder()
                .setOrderId(order.id())
                .setCustomerId(order.executorId())
                .setExecutorId(order.executorId())
                .setAmount(bigDecimalToMoney(order.price()))
                .build();
    }

    public DecreaseCustomerAmountRequest build(DecreaseCustomerAmountRequest request, int counter) {
        return DecreaseCustomerAmountRequest.newBuilder(request)
                .setRetryData(RetryData.newBuilder()
                        .setCounter(counter)
                        .build())
                .build();
    }

    @SneakyThrows
    public DecreaseCustomerAmountRequest fromBytes(byte[] bytes) {
        return DecreaseCustomerAmountRequest.parseFrom(bytes);
    }
}
